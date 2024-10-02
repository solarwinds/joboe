package com.tracelytics.instrumentation;

import java.util.Map;

/**
 *  Subset of org.jboss.remoting.JbossRemoteInvocationResponse
 */
public interface JbossRemoteInvocationResponse {
    public Map getPayload();
}
