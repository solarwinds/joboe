package com.solarwinds.joboe.span.impl;

import java.util.Map;

import com.solarwinds.joboe.XTraceHeader;
import lombok.Getter;

@Getter
public class TraceDecisionParameters {
    private final Map<XTraceHeader, String> headers;
    private final String resource;
    
    public TraceDecisionParameters(Map<XTraceHeader, String> headers, String resource) {
        super();
        this.headers = headers;
        this.resource = resource;
    }


}
