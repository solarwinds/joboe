package com.tracelytics.test.pojo;

import com.appoptics.api.ext.AgentChecker;
import com.appoptics.api.ext.Trace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class TestLog {
    private static final Logger logger = LoggerFactory.getLogger(TestLog.class);

    public static void main(String[] args) {
        AgentChecker.waitUntilAgentReady(10, TimeUnit.SECONDS);
        Trace.startTrace("test-log").report();
        //MDC.put("ao_trace_id", Trace.getCurrentXTraceID());

        logger.info("testing!!!!!");

        Trace.endTrace("test-log");

    }

}
