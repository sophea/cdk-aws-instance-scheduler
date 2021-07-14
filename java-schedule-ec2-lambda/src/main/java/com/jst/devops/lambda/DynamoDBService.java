package com.jst.devops.lambda;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Author: Mak Sophea
 * Date: 05/07/2021
 */
public class DynamoDBService {

    private static final Logger logger = LoggerFactory.getLogger(DynamoDBService.class);
    public static final String SCHEDULE_MODEL = "ScheduleModel";

    private String tableName = SCHEDULE_MODEL;

    public DynamoDBService(String tableName) {
        this.tableName = tableName;
    }
    public DynamoDbClient getClient() {

        // Create a DynamoDbClient object.
        final Region region = Region.AP_SOUTHEAST_1;
        final DynamoDbClient ddb = DynamoDbClient.builder()
                                                 .region(region)
                                                 .build();

        return ddb;
    }

    public List<ScheduleModel> getAllItems() {

        final DynamoDbClient ddb = getClient();

        // Create a DynamoDbEnhancedClient and use the DynamoDbClient object
        DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder()
                                                                      .dynamoDbClient(ddb)
                                                                      .build();
        try {
            // Create a DynamoDbTable object based on ScheduleModel
            DynamoDbTable<ScheduleModel> table = enhancedClient.table(tableName, TableSchema.fromBean(ScheduleModel.class));

            // Get items in the scheduler table.
            final Iterator<ScheduleModel> results = table.scan().items().iterator();
            final ArrayList<ScheduleModel> itemList = new ArrayList();
            results.forEachRemaining(itemList::add);
            return itemList;
        } catch (DynamoDbException e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    // Put an item into a DynamoDB table.
    public void putRecord(DynamoDbEnhancedClient enhancedClient, ScheduleModel item) {

        try {
            // Create a DynamoDbTable object.
            final DynamoDbTable<ScheduleModel> workTable = enhancedClient.table(SCHEDULE_MODEL, TableSchema.fromBean(ScheduleModel.class));

            final String myGuid = java.util.UUID.randomUUID().toString();

            // Populate the table.
            item.setId(myGuid);

            // Put the customer data into a DynamoDB table.
            workTable.putItem(item);

        } catch (DynamoDbException e) {
            logger.error(e.getMessage(), e);
        }
    }
}
