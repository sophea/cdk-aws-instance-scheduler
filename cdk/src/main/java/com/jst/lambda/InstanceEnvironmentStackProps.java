package com.jst.lambda;

import com.jst.lambda.model.EnvironmentData;
import software.amazon.awscdk.core.StackProps;

/**
 * Author: Mak Sophea
 * Date: 05/12/2021
 */
public class InstanceEnvironmentStackProps   {
    private StackProps stackProps;
    private EnvironmentData environmentData;

    public InstanceEnvironmentStackProps(EnvironmentData data) {
        this.environmentData = data;
    }

    public StackProps getStackProps() {
        return stackProps;
    }

    public void setStackProps(StackProps stackProps) {
        this.stackProps = stackProps;
    }

    public EnvironmentData getEnvironmentData() {
        return environmentData;
    }

    public void setEnvironmentData(EnvironmentData environmentData) {
        this.environmentData = environmentData;
    }
}
