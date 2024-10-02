package com.tracelytics.instrumentation.http;

/**
 * Subset of com.opensymphony.xwork2.ActionProxy
 */
public interface StrutsActionProxy {
    public Object getAction();
    public String getActionName();
    public String getMethod();
    public String getNamespace();
}
