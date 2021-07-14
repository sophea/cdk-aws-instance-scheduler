package example;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.entities.Entity;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.jst.devops.lambda.DynamoDBService;
import com.jst.devops.lambda.ScheduleModel;
import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import software.amazon.awssdk.services.dynamodb.waiters.DynamoDbWaiter;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Author: Mak Sophea
 * Date: 05/07/2021
 */
public class DynamoDBServiceTest {

    private static final Gson gson = new GsonBuilder().setPrettyPrinting()
                                                      .create();

    // snippet-start:[dynamodb.java2.create_table.main]
    public static String createTable(DynamoDbClient ddb, String tableName, String key) {

        DynamoDbWaiter dbWaiter = ddb.waiter();
        CreateTableRequest request = CreateTableRequest.builder()
                                                       .attributeDefinitions(AttributeDefinition.builder()
                                                                                                .attributeName(key)
                                                                                                .attributeType(ScalarAttributeType.S)
                                                                                                .build())
                                                       .keySchema(KeySchemaElement.builder()
                                                                                  .attributeName(key)
                                                                                  .keyType(KeyType.HASH)
                                                                                  .build())
                                                       .provisionedThroughput(ProvisionedThroughput.builder()
                                                                                                   .readCapacityUnits(new Long(10))
                                                                                                   .writeCapacityUnits(new Long(10))

                                                                                                   .build())

                                                       .tableName(tableName)
                                                       .build();

        String newTable ="";
        try {
            CreateTableResponse response = ddb.createTable(request);
            DescribeTableRequest tableRequest = DescribeTableRequest.builder()
                                                                    .tableName(tableName)
                                                                    .build();

            // Wait until the Amazon DynamoDB table is created
            WaiterResponse<DescribeTableResponse> waiterResponse =  dbWaiter.waitUntilTableExists(tableRequest);
            waiterResponse.matched().response().ifPresent(System.out::println);

            newTable = response.tableDescription().tableName();
            return newTable;

        } catch (DynamoDbException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
        return "";
    }

    public static void main(String[] args) {

        Entity mySegment = AWSXRay.beginSegment("do-startup-operation");
        AWSXRay.getGlobalRecorder().setTraceEntity(mySegment);
        DynamoDBService dbService = new DynamoDBService(DynamoDBService.SCHEDULE_MODEL);

//        //create table
        final DynamoDbClient ddb = dbService.getClient();
//
//        createTable(ddb, "ScheduleModel", "id");
//
        // Create a DynamoDbEnhancedClient and use the DynamoDbClient object
        DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder()
                                                                      .dynamoDbClient(ddb)
                                                                      .build();


        String scheduleValue ="[{\"days\":[\"mon\",\"tue\",\"wed\",\"thu\",\"fri\"],\"stopTime\":\"18:00\",\"instanceIds\":[\"i-04ac3b83af9972ccc\",\"i-079e49b8f04529acc\",\"i-0aa4bca255ad7f4e6\",\"i-0cf3f4af1a1488fd7\",\"i-093f3ea672017bc9d\",\"i-0a58ce71f23cb22ec\",\"i-0ebf5feba74d3e678\",\"i-07fc87d755dc0dcdb\"]},{\"allDays\":true,\"stopTime\":\"22:00\",\"instanceIds\":[\"i-07fa58acb5f6f1046\",\"i-0f4fed2e1c285d044\"]},{\"allDays\":false,\"days\":[\"mon\",\"tue\",\"wed\",\"thu\",\"fri\"],\"stopTime\":\"00:00\",\"startTime\":\"08:00\",\"instanceIds\":[\"i-0c462d535fd8bbd7d\",\"i-0d62303a89e7839fe\",\"i-0af520a0246b8a3e2\"]},{\"allDays\":false,\"days\":[\"sat\",\"sun\"],\"stopTime\":\"20:00\",\"instanceIds\":[\"i-0c462d535fd8bbd7d\",\"i-0d62303a89e7839fe\",\"i-0af520a0246b8a3e2\"]}]\n";
        final Type listType = new TypeToken<ArrayList<ScheduleModel>>(){}.getType();
        List<ScheduleModel> list = gson.fromJson(scheduleValue, listType);
        for (ScheduleModel item : list) {
            dbService.putRecord(enhancedClient, item);
            System.out.println("ID : " + item.getId());
        }

        for (ScheduleModel item : dbService.getAllItems()) {
            System.out.println(item.getId());
        };

        AWSXRay.endSegment();
    }
}
