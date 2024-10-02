package com.tracelytics.joboe.span.impl;

import java.util.Map;

import com.tracelytics.joboe.XTraceHeader;

public class TraceDecisionParameters {
    private final Map<XTraceHeader, String> headers;
    private final String resource;
    
    public TraceDecisionParameters(Map<XTraceHeader, String> headers, String resource) {
        super();
        this.headers = headers;
        this.resource = resource;
    }
    
    public Map<XTraceHeader, String> getHeaders() {
        return headers;
    }
    
    public String getResource() {
        return resource;
    } 
    
    
}
