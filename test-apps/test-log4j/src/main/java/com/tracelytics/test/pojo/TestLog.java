package com.tracelytics.test.pojo;

import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.slf4j.LoggerFactory;

import com.appoptics.api.ext.AgentChecker;
import com.appoptics.api.ext.Trace;

public class TestLog {
    //private static Logger logger = LogManager.getLogger(TestLog.class);
    private static Logger logger = Logger.getLogger(TestLog.class);
    private static org.slf4j.Logger bridgedLogger = LoggerFactory.getLogger(TestLog.class);

    public static void main(String[] args) throws Exception {
        AgentChecker.waitUntilAgentReady(10, TimeUnit.SECONDS);
        Trace.startTrace("test-log").report();

        logger.info("testing from direct logger!");
        
        bridgedLogger.info("testing from bridged logger!");
        
        Trace.endTrace("test-log");

    }
}
