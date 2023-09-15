package com.tracelytics.util;

import com.tracelytics.lambda.AwsLambdaHostInfoReader;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.ClearEnvironmentVariable;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

import static org.junit.jupiter.api.Assertions.*;

class RuntimeHostInfoReaderProviderTest {

    private final RuntimeHostInfoReaderProvider tested = new RuntimeHostInfoReaderProvider();

    @Test
    @SetEnvironmentVariable(key = "LAMBDA_TASK_ROOT", value = "lambda eh!")
    @SetEnvironmentVariable(key = "AWS_LAMBDA_FUNCTION_NAME", value = "lambda Fn eh!")
    void returnAwsLambdaHostInfoReader() {
        assertTrue(tested.getHostInfoReader() instanceof AwsLambdaHostInfoReader);
    }


    @Test
    @ClearEnvironmentVariable(key = "LAMBDA_TASK_ROOT")
    @ClearEnvironmentVariable(key = "AWS_LAMBDA_FUNCTION_NAME")
    void returnServerHostInfoReader() {
        assertTrue(tested.getHostInfoReader() instanceof ServerHostInfoReader);
    }
}