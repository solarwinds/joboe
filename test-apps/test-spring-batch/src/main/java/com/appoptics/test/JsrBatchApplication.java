package com.appoptics.test;

import com.appoptics.api.ext.AgentChecker;

import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import java.util.Properties;
import java.util.concurrent.TimeUnit;


public class JsrBatchApplication {
    public static void main(String[] args) {
        JobOperator operator = BatchRuntime.getJobOperator();
        operator.start("flowJobSequence", new Properties());
    }
}
