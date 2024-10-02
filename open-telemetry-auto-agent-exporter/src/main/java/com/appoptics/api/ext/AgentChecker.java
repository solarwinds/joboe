package com.appoptics.api.ext;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.tracelytics.agent.Agent;
import com.tracelytics.agent.Agent.AgentStatus;
import com.tracelytics.joboe.StartupManager;

/**
 * Checker to ensure agent is available and ready to report data
 * @author pluk
 *
 */
public class AgentChecker {
    private static Logger logger = Logger.getLogger("agent-sdk");

    /**
     * Blocks until agent is ready (established connection with data collector) or timeout expired. 
     * 
     * Take note that if an agent is not ready, traces and metrics collected will not be processed.
     * 
     * Call this method to ensure agent is ready before reporting traces for one-off batch jobs
     *     
     * @param timeout
     * @param unit
     * @return  whether the agent is ready
     */
    public static boolean waitUntilAgentReady(long timeout, TimeUnit unit) {
        try {
            StartupManager.isAgentReady().get(timeout, unit);
            return true;
        } catch (Exception e) {
            logger.warning("Agent is still not ready after waiting for " + timeout + " " + unit);
            return false;
        }
    }
}
