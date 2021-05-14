package com.tracelytics.instrumentation.http;

import java.util.HashMap;
import java.util.Stack;

/**
 * These methods are accessed from ServletInstrumentation
 * Subset of javax.servlet.http.HttpServletResponse/jakarta.servlet.http.HttpServletResponse, with added methods for storing extra data
 *
 */
public interface HttpServletResponse {
    void addHeader(String name, String value);
    void setHeader(String name, String value);
    boolean containsHeader(String name);

    // Added in ServletResponseInstrumentation:
    int tlysGetStatus();
    void tlysSetXTraceID(String xtrace_id);
    String tlysGetXTraceID();
    int tlysReqCount();
    int tlysIncReqCount();
    int tlysDecReqCount();

    // Used for tracking when request is within framework code:
    public HashMap tlysGetFrameworkCounterMap();
    public Stack tlysGetFrameworkLayerStack();
}
