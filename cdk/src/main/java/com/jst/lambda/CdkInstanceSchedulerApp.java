package com.jst.lambda;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.jst.lambda.model.EnvironmentData;
import software.amazon.awscdk.core.App;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public final class CdkInstanceSchedulerApp {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static void main(final String[] args) {

        final App app = new App();
        // -c ENV=[name]
        final String env = (String) app.getNode().tryGetContext("ENV");

        if (env == null) {
            System.out.println("ENV is required, use: -c ENV=key. cdk deloy -c ENV=[dev,sit,uat,prod]");
            System.exit(1);
        }

        new CdInstanceSchedulerStack(app, "CdkInstanceModelStack", new InstanceEnvironmentStackProps(loadEnvironments(env)));
        app.synth();
    }

    public static EnvironmentData loadEnvironments(String env) {
        List<EnvironmentData> environmentDataList = null;
        try (InputStream stream = CdkInstanceSchedulerApp.class.getResourceAsStream("/env.json")) {

            final Type listType = new TypeToken<ArrayList<EnvironmentData>>() {
            }.getType();

            environmentDataList = gson.fromJson(new InputStreamReader(stream), listType);

            for (EnvironmentData item : environmentDataList) {
                if (env.equalsIgnoreCase(item.name)) {
                    return item;
                }
            }

        } catch (IOException e) {
//            LOGGER.error(e.getMessage(), e);
        }
        return null;
    }
}
