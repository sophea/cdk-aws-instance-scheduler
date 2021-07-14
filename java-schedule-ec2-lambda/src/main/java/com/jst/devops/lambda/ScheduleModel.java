package com.jst.devops.lambda;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.io.Serializable;
import java.util.List;

/**
 * Author: Mak Sophea
 * Date: 04/28/2021
 */


//[{ "days":["mon","tue","wed","thu","fri","sat"],"stopTime":"xxx","startTime":"16:00","instanceIds":["id1","id2"]},{"allDay":1,"stopTime":"xxx2","startTime":"14:50","instanceIds":["id12","id22"]}]
@DynamoDbBean
public class ScheduleModel  implements Serializable {
    private String id;
    private String stopTime;
    private String startTime;
    //["mon","tue","wed","thu","fri","sat","sun"]
    private List<String> days;
    /**allDays if true will override all*/
    private boolean allDays;

    @DynamoDbPartitionKey
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    private List<String> instanceIds;

    public String getStopTime() {
        return stopTime;
    }

    public void setStopTime(String stopTime) {
        this.stopTime = stopTime;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public List<String> getInstanceIds() {
        return instanceIds;
    }

    public void setInstanceIds(List<String> instanceIds) {
        this.instanceIds = instanceIds;
    }

    public List<String> getDays() {
        return days;
    }

    public void setDays(List<String> days) {
        this.days = days;
    }

    public boolean isAllDays() {
        return allDays;
    }

    public void setAllDays(boolean allDays) {
        this.allDays = allDays;
    }
}
