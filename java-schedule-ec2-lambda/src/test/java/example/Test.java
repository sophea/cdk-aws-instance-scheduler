package example;

import com.fasterxml.jackson.databind.ser.std.BooleanSerializer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.jst.devops.lambda.EC2Instance;
import com.jst.devops.lambda.ScheduleModel;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;

import java.lang.reflect.Type;
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
public class Test {
    private static final Logger logger = LoggerFactory.getLogger(Handler.class);
    private static final Gson gson = new GsonBuilder().setPrettyPrinting()
                                                      .create();

    private static final String PHNOM_PENH_TIME_ZONE = "Asia/Phnom_Penh";
//    private static final DateTimeZone ASIA_PHNOM_PENH = DateTimeZone.forID(PHNOM_PENH_TIME_ZONE);

    public static void main(String[] args) {
        String scheduleValue ="[{\"days\":[\"mon\",\"tue\",\"wed\",\"thu\",\"fri\",\"sat\"],\"stopTime\":\"18:00\",\"startTime\":\"01:55\",\"instanceIds\":[\"id1\",\"id2\"]},{\"allDays\":true,\"stopTime\":\"20:00\",\"startTime\":\"14:50\",\"instanceIds\":[\"id12\",\"id22\"]}]";
        System.out.println("value : " +  scheduleValue);
        final Type listType = new TypeToken<ArrayList<ScheduleModel>>(){}.getType();



        LocalDate localDate = LocalDate.now(ZoneId.of(PHNOM_PENH_TIME_ZONE));
        LocalTime localTime = LocalTime.now(ZoneId.of(PHNOM_PENH_TIME_ZONE));

        DayOfWeek dow = localDate.getDayOfWeek();
        String dayName = dow.getDisplayName(TextStyle.SHORT, Locale.US).toLowerCase(); // String = Tue
        List<ScheduleModel> list = gson.fromJson(scheduleValue, listType);
        logger.info("Object : " +  gson.toJson(list));

        for (ScheduleModel scheduleModel: list) {

            final boolean allDay = scheduleModel.isAllDays();
            final boolean dayIsGranted = scheduleModel.getDays() != null &&scheduleModel.getDays().contains(dayName);

            if (allDay || dayIsGranted) {
                final String startTime = scheduleModel.getStartTime();
                //convert to local time
                LocalTime sTime = LocalTime.parse(startTime);
                //   System.out.println(sTime);
                long value =Math.abs(ChronoUnit.MINUTES.between(sTime, localTime));
                if (value >=0 && value <=5) {
                    //proceed the function
                }
                System.out.println(localTime + ", " + sTime + " === " + value);


            }
        }

//        final Map<String,Object> message = new HashMap<>();
//        message.put("action","stop");
//        message.put("ids", Arrays.asList("id1","id2"));
//        SqsClient sqsClient = SqsClient.builder().region(Region.AP_SOUTHEAST_1).build();
//
//        EC2Instance.sendMessageSQS(sqsClient, "sqs-ec2-stopstart", gson.toJson(message));
    }
}
