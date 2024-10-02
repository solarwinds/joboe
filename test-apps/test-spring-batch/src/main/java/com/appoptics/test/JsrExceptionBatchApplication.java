package com.appoptics.test;

import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import java.util.Properties;


public class JsrExceptionBatchApplication {
    public static void main(String[] args) {
        JobOperator operator = BatchRuntime.getJobOperator();
        operator.start("exceptionJobSequence", new Properties());
    }
}
