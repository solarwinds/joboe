package com.tracelytics.util;

import com.tracelytics.joboe.rpc.HostType;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.ClearEnvironmentVariable;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

import static org.junit.jupiter.api.Assertions.*;

class HostTypeDetectorTest {

    @Test
    void testGetHostTypeWhenLambdaEnvsAreNotSet() {
        assertEquals(HostType.PERSISTENT, HostTypeDetector.getHostType());
    }

    @Test
    @SetEnvironmentVariable(key = "LAMBDA_TASK_ROOT", value = "lambda eh!")
    @SetEnvironmentVariable(key = "AWS_LAMBDA_FUNCTION_NAME", value = "lambda Fn eh!")
    void testGetHostTypeWhenLambdaEnvsAreSet() {
        assertEquals(HostType.AWS_LAMBDA, HostTypeDetector.getHostType());
    }

    @Test
    @SetEnvironmentVariable(key = "LAMBDA_TASK_ROOT", value = "lambda eh!")
    @SetEnvironmentVariable(key = "AWS_LAMBDA_FUNCTION_NAME", value = "lambda Fn eh!")
    void testIsLambdaReturnTrueWhenLambdaEnvsAreSet() {
        assertTrue(HostTypeDetector.isLambda());
    }

    @Test
    @ClearEnvironmentVariable(key = "LAMBDA_TASK_ROOT")
    @ClearEnvironmentVariable(key = "AWS_LAMBDA_FUNCTION_NAME")
    void testIsLambdaReturnFalseWhenLambdaEnvsAreNotSet() {
        assertFalse(HostTypeDetector.isLambda());
    }
}