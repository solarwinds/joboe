package com.tracelytics.test.pojo;

import com.appoptics.api.ext.AgentChecker;
import com.appoptics.api.ext.Trace;
import com.appoptics.api.ext.TraceEvent;

import java.util.concurrent.TimeUnit;

public class TestBigTrace {
    public static void main(String[] args) {
        AgentChecker.waitUntilAgentReady(10, TimeUnit.SECONDS);
        String spanName = "big-trace";
        String VALUE = getLongString(1024);
        Trace.startTrace(spanName).report();
        for (int i = 0 ; i < 1000; i ++) {
            TraceEvent infoEvent = Trace.createInfoEvent(spanName);
            for (int j = 0 ; j < 10; j ++) {
                infoEvent.addInfo("key" + j, VALUE);
            }
            infoEvent.report();
        }
        System.out.println("!!!!!!!!!!!!!!" + Trace.endTrace(spanName));
    }

    private static String getLongString(int length) {
        StringBuilder result = new StringBuilder();
        for (int i = 0 ; i < length; i ++) {
            result.append('a');
        }
        return result.toString();
    }
}
