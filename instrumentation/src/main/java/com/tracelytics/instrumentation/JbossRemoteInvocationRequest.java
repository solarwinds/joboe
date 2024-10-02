package com.tracelytics.instrumentation;

import java.util.Map;

/**
 * Subset of org.jboss.remoting.InvocationRequest
 */
public interface JbossRemoteInvocationRequest {

    public Map getRequestPayload();
    public void setRequestPayload(Map requestPayload);
    
    public Map getReturnPayload();
    public void setReturnPayload(Map returnPayload);

    public String getSubsystem();
    public Object getParameter();

}
