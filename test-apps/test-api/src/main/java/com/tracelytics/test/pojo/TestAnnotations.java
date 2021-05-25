package com.tracelytics.test.pojo;

import com.appoptics.api.ext.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class TestAnnotations {
    public static void main(String[] args) throws InterruptedException {
        AgentChecker.waitUntilAgentReady(10, TimeUnit.SECONDS);
        Trace.startTrace("test-annotations").report();

        try {
            method1();
        } catch (RuntimeException e) {
            //expected
        }
        method2();
        method3();

        Trace.endTrace("test-annotations");
    }

    @LogMethod(reportExceptions = true)
    private static void method1() {
        throw new RuntimeException("Testing");
    }

    @ProfileMethod(profileName = "method2")
    private static void method2() {

    }

    @LogMethod(layer = "custom-method-name", backTrace = true, storeReturn = true)
    @ProfileMethod(profileName = "custom-profile-name", backTrace = true, storeReturn = true)
    private static Integer method3() {
        return 42;
    }
}
