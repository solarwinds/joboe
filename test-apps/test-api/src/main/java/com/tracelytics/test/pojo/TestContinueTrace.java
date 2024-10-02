package com.tracelytics.test.pojo;

import com.appoptics.api.ext.Trace;

public class TestContinueTrace {
    public static void main(String[] args) {
        //continue with valid x-trace ID
        Trace.continueTrace("test-continue", "2B0123456789012345678901234567890123456789012345678901234561").report();
        System.out.println("Should start with 2B01234567890...");
        System.out.println(Trace.getCurrentXTraceID());
        //continue with all zeroes
        Trace.continueTrace("test-continue", "2B0000000000000000000000000000000000000000000000000000000000").report();
        System.out.println("Should start with a valid task ID, (not all 0)");
        System.out.println(Trace.getCurrentXTraceID());
        //continue with invalid x-trace ID
        Trace.continueTrace("test-continue", "2Bxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx").report();
        System.out.println("Should start with a valid task ID, (not xxxx)");
        System.out.println(Trace.getCurrentXTraceID());
    }
}
