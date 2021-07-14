package example;

import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Author: Mak Sophea
 * Date: 05/05/2021
 */
public class SendMessagesTest {
    private static final Logger logger = LoggerFactory.getLogger(SendMessagesTest.class);
    private static final Gson gson = new GsonBuilder()
        .registerTypeAdapter(SQSEvent.class, new SQSEventDeserializer())
        .setPrettyPrinting()
        .create();

//    public static AWSCredentials amazonAWSCredentials() {
//        return new ProfileCredentialsProvider("default").getCredentials();
//        //return new BasicAWSCredentials(amazonAWSAccessKey, amazonAWSSecretKey);
//    }

    public static void main(String[] args) {


        final Map<String, Object> message = new HashMap<>();
        message.put("action", "stop");
        message.put("ids", Arrays.asList("id1", "id2"));


//        .standard().withCredentials(amazonAWSCredentialsProvider())
//                   .withRegion(Regions.AP_SOUTHEAST_1).build();


//        AWSCredentialsProvider awsCredentialsProvider = new AWSStaticCredentialsProvider(amazonAWSCredentials());

        //  SqsClient.builder().
        SqsClient sqsClient = SqsClient.builder().region(Region.AP_SOUTHEAST_1)
                                       //.credentialsProvider(awsCredentialsProvider)
                                       .build();

        //    EC2Instance.sendMessageSQS(sqsClient, "sqs-ec2-stopstart", gson.toJson(message));


        String queueName = "sqs-ec2-stopstart";
        sendMessage(sqsClient, queueName, gson.toJson(message));
        sqsClient.close();
    }

    // snippet-start:[sqs.java2.send_recieve_messages.main]
    public static void sendMessage(SqsClient sqsClient, String queueName, String message) {

        try {
            CreateQueueRequest request = CreateQueueRequest.builder()
                                                           .queueName(queueName)
                                                           .build();
            CreateQueueResponse createResult = sqsClient.createQueue(request);

            GetQueueUrlRequest getQueueRequest = GetQueueUrlRequest.builder()
                                                                   .queueName(queueName)
                                                                   .build();

            String queueUrl = sqsClient.getQueueUrl(getQueueRequest).queueUrl();

            SendMessageRequest sendMsgRequest = SendMessageRequest.builder()
                                                                  .queueUrl(queueUrl)
                                                                  .messageBody(message)
                                                                  .delaySeconds(5)
                                                                  .build();
            sqsClient.sendMessage(sendMsgRequest);

        } catch (SqsException e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            System.exit(1);
        }
    }
}
