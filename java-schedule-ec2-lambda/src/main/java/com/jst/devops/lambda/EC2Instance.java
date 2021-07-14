package com.jst.devops.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SqsException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Author: Mak Sophea
 * Date: 04/28/2021
 */

public class EC2Instance implements RequestHandler<Map<String, Object>, String> {
    private static final Logger logger = LoggerFactory.getLogger(EC2Instance.class);
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private static final Ec2Client ec2Client = Ec2Client.builder().region(Region.AP_SOUTHEAST_1).build();

    private static final SqsClient sqsClient = SqsClient.builder()
                                                        .region(Region.AP_SOUTHEAST_1)
                                                        .build();

    private static final String PHNOM_PENH_TIME_ZONE = "Asia/Phnom_Penh";
    private static final String QUEUE_NAME = System.getenv("QUEUE_NAME");
    private static final String TABLE_NAME = System.getenv("TABLE_NAME");

    @Override
    public String handleRequest(Map<String, Object> event, Context context) {

        final LocalDate localDate = LocalDate.now(ZoneId.of(PHNOM_PENH_TIME_ZONE));
        final LocalTime localTime = LocalTime.now(ZoneId.of(PHNOM_PENH_TIME_ZONE));
        logger.info("{} localDate {} , local time {}", PHNOM_PENH_TIME_ZONE, localDate, localTime);

        //logger.info("ENVIRONMENT VARIABLES: " + gson.toJson(System.getenv()));
        //logger.info("CONTEXT: " + gson.toJson(context));
        // process event
        //logger.info("EVENT: " + gson.toJson(event));

        final long t = System.currentTimeMillis();

        final DynamoDBService dynamoDBService = new DynamoDBService(TABLE_NAME);
        final List<ScheduleModel> list = dynamoDBService.getAllItems();
        logger.info("data found {}", gson.toJson(list));

        final long last = System.currentTimeMillis() - t;
        mainProcess(localDate, localTime, list);
        logger.info("Take times : {} millis", last);
        return "OK";
    }

    private void mainProcess(LocalDate localDate, LocalTime localTime, List<ScheduleModel> list) {

        final DayOfWeek dow = localDate.getDayOfWeek();
        final String dayName = dow.getDisplayName(TextStyle.SHORT, Locale.US).toLowerCase(); // String = Tue
        for (ScheduleModel scheduleModel : list) {

            final boolean allDays = scheduleModel.isAllDays();
            final boolean dayIsGranted = scheduleModel.getDays() != null && scheduleModel.getDays().contains(dayName);
            logger.info(" allDays {} || dayName [{}] is dayIsGranted {} ", allDays, dayName, dayIsGranted);
            if (allDays || dayIsGranted) {
                final String startTime = scheduleModel.getStartTime();
                if (startTime != null) {
                    //convert to local time start
                    try {
                        final LocalTime sTime = LocalTime.parse(startTime);
                        long value = Math.abs(ChronoUnit.MINUTES.between(sTime, localTime));
                        if (value >= 0 && value <= 15) {
                            logger.info("Processing startTime {}", startTime);
                            //proceed the function
//                        for (String id : scheduleModel.getInstanceIds()) {
//                            startInstance(ec2Client, id);
//                        }
                            final Map<String, Object> message = new HashMap<>();
                            message.put("action", "start");
                            message.put("ids", scheduleModel.getInstanceIds());
                            sendMessageSQS(sqsClient, QUEUE_NAME, gson.toJson(message));
                            continue;
                        }
                    } catch (Exception e) {
                        logger.warn("wrong format startTime value {} , skip this record", startTime);
                    }
                }

                try {
                    //stop time
                    LocalTime stopTime = LocalTime.parse(scheduleModel.getStopTime());
                    long value = Math.abs(ChronoUnit.MINUTES.between(stopTime, localTime));
                    if (value >= 0 && value <= 15) {
                        //proceed the function
                        logger.info("Processing stopTime {}", stopTime);

//                        for (String id : scheduleModel.getInstanceIds()) {
//                            stoptInstance(ec2Client, id);
//                        }
                        final Map<String, Object> message = new HashMap<>();
                        message.put("action", "stop");
                        message.put("ids", scheduleModel.getInstanceIds());
                        //  logger.info("body json {}", gson.toJson(message));
                        sendMessageSQS(sqsClient, QUEUE_NAME, gson.toJson(message));
                    }
                } catch (Exception e) {
                    logger.warn("wrong format startTime value {} , skip this record", scheduleModel.getStopTime());
                }
            }
        }
    }
    private static void sendMessageSQS(SqsClient sqsClient, String queueName, String message) {
        // wrap in subsegment
       // final Subsegment subsegment = AWSXRay.beginSubsegment("send-message");
        try {

//            CreateQueueRequest request = CreateQueueRequest.builder()
//                                                           .queueName(queueName)
//                                                           .build();
//            CreateQueueResponse createResult = sqsClient.createQueue(request);

            GetQueueUrlRequest getQueueRequest = GetQueueUrlRequest.builder()
                                                                   .queueName(queueName)
                                                                   .build();

            final String queueUrl = sqsClient.getQueueUrl(getQueueRequest).queueUrl();

            SendMessageRequest sendMsgRequest = SendMessageRequest.builder()
                                                                  .queueUrl(queueUrl)
                                                                  .messageBody(message)
                                                                  .delaySeconds(3)
                                                                  .build();

            sqsClient.sendMessage(sendMsgRequest);
            logger.info("============sending to sqs :{}, with message body {} ", queueName, message);

        } catch (SqsException e) {
            logger.info(e.awsErrorDetails().errorMessage(), e);
            //  subsegment.addException(e);
        }
//        } finally {
//            AWSXRay.endSubsegment();
//        }
    }

    private List<EC2Model> loadAllEC2Instances() {
        final List<EC2Model> result = new ArrayList<>();
        final DescribeInstancesResponse listingsResult = ec2Client.describeInstances();
        for (Reservation reservation : listingsResult.reservations()) {
            for (Instance instance : reservation.instances()) {
                result.add(convert(instance));
            }
        }
        return result;
    }
    public String stoptInstance(Ec2Client ec2Client, String instanecID) {
        StopInstancesRequest stopInstancesRequest = StopInstancesRequest.builder().instanceIds(instanecID).build();

        ec2Client.stopInstances(stopInstancesRequest)
                 .stoppingInstances()
                 .get(0)
                 .previousState()
                 .name();
        String result = String.format("Stopping the instance with ID %s", instanecID);
        logger.info(result);
        return result;
    }

    public String startInstance(Ec2Client ec2Client, String instanecID) {
        final StartInstancesRequest startInstancesRequest = StartInstancesRequest.builder()
                                                                                 .instanceIds(instanecID)
                                                                                 .build();

        ec2Client.startInstances(startInstancesRequest)
                 .startingInstances()
                 .get(0)
                 .previousState()
                 .name();
        String result = String.format("Starting the Instance with ID %s", instanecID);
        logger.info(result);
        return result;
    }

    private EC2Model convert(Instance instance) {
        final EC2Model ec2Instance = new EC2Model();

        ec2Instance.setInstanceId(instance.instanceId());
        ec2Instance.setInstanceType(instance.instanceType().name());
        ec2Instance.setInstanceState(instance.state().nameAsString());
        ec2Instance.setPrivateIp(instance.privateIpAddress());
        ec2Instance.setRegion(instance.placement().availabilityZone());
        ec2Instance.setPlatform(instance.platformAsString() == null ? "-" : instance.platformAsString());
        final Tag tag = instance.tags().stream().filter(t -> "Name".equals(t.key())).findAny().orElse(null);
        ec2Instance.setName(tag.value());

        return ec2Instance;
    }

}
