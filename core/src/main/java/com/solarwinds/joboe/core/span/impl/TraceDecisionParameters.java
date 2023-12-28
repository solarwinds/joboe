package com.solarwinds.joboe.core.span.impl;

import java.util.Map;

import com.solarwinds.joboe.core.XTraceHeader;
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
