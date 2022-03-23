package com.jst.lambda;

import com.jst.lambda.model.EnvironmentData;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Duration;
import software.amazon.awscdk.core.RemovalPolicy;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.services.dynamodb.Attribute;
import software.amazon.awscdk.services.dynamodb.AttributeType;
import software.amazon.awscdk.services.dynamodb.Table;
import software.amazon.awscdk.services.dynamodb.TableProps;
import software.amazon.awscdk.services.events.EventPattern;
import software.amazon.awscdk.services.events.Rule;
import software.amazon.awscdk.services.events.Schedule;
import software.amazon.awscdk.services.events.targets.LambdaFunction;
import software.amazon.awscdk.services.iam.Effect;
import software.amazon.awscdk.services.iam.IRole;
import software.amazon.awscdk.services.iam.ManagedPolicy;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.lambda.*;
import software.amazon.awscdk.services.lambda.eventsources.SqsEventSource;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.amazon.awscdk.services.sqs.Queue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CdInstanceSchedulerStack extends Stack {
    private static final int TIMEOUT = 60;

    private final String TABLE_NAME = "ScheduleModel";


    public CdInstanceSchedulerStack(final Construct parent, final String id, final InstanceEnvironmentStackProps instanceEnvironmentStackProps) {
        super(parent, id, instanceEnvironmentStackProps.getStackProps());


        final EnvironmentData environmentData = instanceEnvironmentStackProps.getEnvironmentData();

        final String environmentName = environmentData.name;
        final String region = environmentData.data.getRegion();
        final Queue sqs = software.amazon.awscdk.services.sqs.Queue.Builder.create(this, "sqs-ec2-stopstart")
                                                                           .visibilityTimeout(Duration.seconds(TIMEOUT))
                                                                           .removalPolicy(RemovalPolicy.DESTROY)
                                                                           .queueName("sqs-ec2-stopstart").build();

        // Defines a new lambda resource
        final Function sqsLambda = Function.Builder.create(this, "SqsInstancesStopStartLambda")
                                                   .runtime(Runtime.PYTHON_3_8)    // execution environment
                                                   .code(Code.fromAsset("../lambda/functions"))  // code loaded from the "lambda" directory
                                                   .handler("stop_start_ec2.lambda_handler")        // file is "index", function is "handler"
                                                   .timeout(Duration.seconds(TIMEOUT))
                                                   .description("Lambda python stop/start EC2 trigger by sqs")
                                                   .logRetention(RetentionDays.ONE_WEEK)
                                                   .memorySize(256)
                                                   .tracing(Tracing.ACTIVE)
                                                   .build();
        //set environment
        sqsLambda.addEnvironment("REGION", region);
        sqsLambda.addEnvironment("ENV", environmentName.toUpperCase()); //DEV, SIT, UAT, PROD
        //add layer version
//        final LayerVersion layerVersion = LayerVersion.Builder.create(this, "python-lib-xray").layerVersionName("py-lib").code(Code.fromAsset("../lambda/package"))
//                                                              .compatibleRuntimes(Arrays.asList(Runtime.PYTHON_3_8)).build();
        sqsLambda.addLayers(pythonXrayLayer("sqsLambda"));

        //attach sqs to lambda
        final software.amazon.awscdk.services.lambda.eventsources.SqsEventSource sqsEventSource = SqsEventSource.Builder.create(sqs).build();
        sqsLambda.addEventSource(sqsEventSource);

        //add permissions
        sqsLambda.addToRolePolicy(PolicyStatement.Builder.create()
                                                         .effect(Effect.ALLOW)
                                                         .actions(Arrays.asList("ec2:StartInstances", "ec2:StopInstances","kms:*"))
                                                         .resources(Arrays.asList("*"))
                                                         .build());



        buildLambdaInstanceSchedule(sqs, environmentData);

        buildLambdaEC2ChangeStateEvent(environmentData);


    }

    private LayerVersion pythonXrayLayer(String id) {
        final LayerVersion layerVersion = LayerVersion.Builder.create(this, "python-lib-xray-" + id)
                                                              .layerVersionName("py-lib")
                                                              .code(Code.fromAsset("../lambda/package"))
                                                              .compatibleRuntimes(Arrays.asList(Runtime.PYTHON_3_8))
                                                              .build();
        return layerVersion;
    }
    public void buildLambdaInstanceSchedule(Queue sqs, EnvironmentData environmentData) {
        // Defines a new lambda resource with layer
        final Function javaScheduleLambda = Function.Builder.create(this, "InstanceScheduleEC2Lambda")
                                                            .runtime(Runtime.JAVA_11)    // execution environment
                                                            .code(Code.fromAsset("./../assets/ec2-lambda-java.jar"))  // code loaded from the "lambda" directory
                                                            .handler("com.jst.devops.lambda.EC2Instance::handleRequest")        // file is "index", function is "handler"
                                                            .timeout(Duration.seconds(TIMEOUT))
                                                            .description("instance schedule EC2 lambda java")
                                                            .logRetention(RetentionDays.ONE_WEEK)
                                                            .memorySize(512)

                                                            .tracing(Tracing.ACTIVE)
                                                            .build();

        //set environment QUEUE_NAME: sqs-ec2-stopstart
        javaScheduleLambda.addEnvironment("QUEUE_NAME", sqs.getQueueName());
        javaScheduleLambda.addEnvironment("ENV", environmentData.name.toUpperCase()); //DEV, SIT, UAT, PROD
        javaScheduleLambda.addEnvironment("TABLE_NAME", TABLE_NAME);
        //add layer
        final LayerVersion javaLayer = LayerVersion.Builder.create(this, "ec2-lambda-java-lib-mvn")
                                                           .layerVersionName("ec2-lambda-java-lib-mvn")
                                                           .code(Code.fromAsset("../assets/ec2-lambda-java-lib-mvn.zip"))
                                                           .compatibleRuntimes(Arrays.asList(Runtime.JAVA_8, Runtime.JAVA_11))
                                                           .build();
        javaScheduleLambda.addLayers(javaLayer);


        //add permission
        javaScheduleLambda.addToRolePolicy(PolicyStatement.Builder.create()
                                                                  .effect(Effect.ALLOW)
                                                                  .actions(Arrays.asList("ec2:StartInstances",
                                                                                         "ec2:StopInstances",
                                                                                         "sqs:SendMessage",
                                                                                         "sqs:GetQueueUrl",
                                                                                         "dynamodb:GetItem",
                                                                                         "dynamodb:Query",
                                                                                         "dynamodb:PutItem",
                                                                                         "dynamodb:UpdateItem",
                                                                                         "dynamodb:BatchWriteItem",
                                                                                         "dynamodb:Scan"
                                                                  ))
                                                                  .resources(Arrays.asList("*"))
                                                                  .build());

        // javaScheduleLambda.addToRolePolicy(PolicyStatement.fromJson());

        IRole myRole = javaScheduleLambda.getRole();
        myRole.addManagedPolicy(ManagedPolicy.fromAwsManagedPolicyName("service-role/AWSLambdaVPCAccessExecutionRole"));
        myRole.addManagedPolicy(ManagedPolicy.fromAwsManagedPolicyName("service-role/AWSLambdaBasicExecutionRole"));
        myRole.addManagedPolicy(ManagedPolicy.fromAwsManagedPolicyName("AWSLambda_ReadOnlyAccess"));
        myRole.addManagedPolicy(ManagedPolicy.fromAwsManagedPolicyName("AWSXrayWriteOnlyAccess"));
        myRole.addManagedPolicy(ManagedPolicy.fromAwsManagedPolicyName("AWSLambda_ReadOnlyAccess"));
        myRole.addManagedPolicy(ManagedPolicy.fromAwsManagedPolicyName("AmazonEC2ReadOnlyAccess"));
        myRole.addManagedPolicy(ManagedPolicy.fromAwsManagedPolicyName("AmazonEC2ReadOnlyAccess"));

        final TableProps tableProps;
        final Attribute partitionKey = Attribute.builder()
                                                .name("id")
                                                .type(AttributeType.STRING)
                                                .build();
        tableProps = TableProps.builder()
                               .tableName(TABLE_NAME)
                               .partitionKey(partitionKey)
                               // The default removal policy is RETAIN, which means that cdk destroy will not attempt to delete
                               // the new table, and it will remain in your account until manually deleted. By setting the policy to
                               // DESTROY, cdk destroy will delete the table (even if it has data in it)
                               .removalPolicy(RemovalPolicy.DESTROY)
                               .build();
        final Table dynamodbTable = new Table(this, TABLE_NAME, tableProps);

        dynamodbTable.grantReadWriteData(javaScheduleLambda);


        //EventBridge every 15min to lambda
        // Create EventBridge rule that will execute our Lambda every 2 minutes
        Rule ruleScheduled = Rule.Builder.create(this, "lambda-instance-schedule")
                                         //.schedule(Schedule.expression("rate(15 minutes)"))
                                         .schedule(Schedule.expression("cron(0/30 * * * ? *)"))  //every 30min
                                         .build();

        // Set the target of our EventBridge rule to our Lambda function
        ruleScheduled.addTarget(new LambdaFunction(javaScheduleLambda));

    }


    public void buildLambdaEC2ChangeStateEvent(EnvironmentData environmentData) {
        /**Lambda change stage ec2 instance*/
        // Defines a new lambda resource
        final Function ec2ChangeStateLambda = Function.Builder.create(this, "ec2ChangeStateLambda")
                                                              .runtime(Runtime.PYTHON_3_8)    // execution environment
                                                              .code(Code.fromAsset("../lambda/ec2-change-state"))  // code loaded from the "lambda" directory
                                                              .handler("ec2_change_state.lambda_handler")        // file is "index", function is "handler"
                                                              .timeout(Duration.seconds(TIMEOUT))
                                                              .description("EC2 changes state")
                                                              .logRetention(RetentionDays.ONE_WEEK)
                                                              .memorySize(256)
                                                              .tracing(Tracing.ACTIVE)
                                                              .build();

        //set environment QUEUE_NAME: sqs-ec2-stopstart
        ec2ChangeStateLambda.addEnvironment("HookUrl", environmentData.data.getHookUrl());
        ec2ChangeStateLambda.addEnvironment("ENV", environmentData.name.toUpperCase()); //DEV, SIT, UAT, PROD


        //  LayerVersion layerVersion = LayerVersion.Builder.create(this, "python-lib-2").layerVersionName("py-lib").code(Code.fromAsset("../lambda/package")).build();

        ec2ChangeStateLambda.addLayers(pythonXrayLayer("ec2ChangeStateLambda"));

        final IRole ec2ChangeStateLambdaRole = ec2ChangeStateLambda.getRole();
        ec2ChangeStateLambdaRole.addManagedPolicy(ManagedPolicy.fromAwsManagedPolicyName("service-role/AWSLambdaVPCAccessExecutionRole"));
        ec2ChangeStateLambdaRole.addManagedPolicy(ManagedPolicy.fromAwsManagedPolicyName("service-role/AWSLambdaBasicExecutionRole"));
        ec2ChangeStateLambdaRole.addManagedPolicy(ManagedPolicy.fromAwsManagedPolicyName("AWSLambda_ReadOnlyAccess"));
        ec2ChangeStateLambdaRole.addManagedPolicy(ManagedPolicy.fromAwsManagedPolicyName("AWSXrayWriteOnlyAccess"));
        ec2ChangeStateLambdaRole.addManagedPolicy(ManagedPolicy.fromAwsManagedPolicyName("AWSLambda_ReadOnlyAccess"));
        ec2ChangeStateLambdaRole.addManagedPolicy(ManagedPolicy.fromAwsManagedPolicyName("AmazonEC2ReadOnlyAccess"));

//        {
//            "source": ["aws.ec2"],
//            "detail-type": ["EC2 Instance State-change Notification"],
//            "detail": {
//            "state": ["stopped", "running", "shutting-down", "terminated"]
//        }

        final Map<String, List<String>> stateMap = new HashMap<>();
        stateMap.put("state", Arrays.asList("stopped", "running", "shutting-down", "terminated"));
        Rule ec2ChangeRle = Rule.Builder.create(this, "ec2-change-state")
                                        .eventPattern(EventPattern.builder()
                                                                  .source(Arrays.asList("aws.ec2"))
                                                                  .detailType(Arrays.asList("EC2 Instance State-change Notification"))
                                                                  .detail(stateMap)
                                                                  .build())

                                        .build();


        LambdaFunction lf = LambdaFunction.Builder.create(ec2ChangeStateLambda)
//                                                  .event(targetInput)
                                                  .build();

        //Set the target of our EventBridge rule to our Lambda function
        ec2ChangeRle.addTarget(lf);

        //TODOs : manual apply targetInput Config as Transformation in AWS Console manually
//        {"account":"$.account","instance-id":"$.detail.instance-id","region":"$.region","state":"$.detail.state","time":"$.time"}
//        {"instance-id":"<instance-id>","state":"<state>","time":"<time>","region":"<region>","account":"<account>",
//            "message": "At <time>, the status of your EC2 instance <instance-id> on account <account> in the AWS Region <region> has changed to <state>."
//        }

    }


}
