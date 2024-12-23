package com.solarwinds.joboe.sampling;

import java.util.*;
import java.util.Map.Entry;


/**
 * Computes the response from {@link XTraceOptions} processing.
 *
 * Contains the key/value produced from the computation.
 */
public class XTraceOptionsResponse {
    
    
    public static XTraceOptionsResponse computeResponse(XTraceOptions options, TraceDecision traceDecision, boolean isServiceRoot) {
        if (options == null) {
            return null;
        }

        XTraceOptionsResponse response = new XTraceOptionsResponse();

        if (options.getAuthenticationStatus().isFailure()) { //if auth failure, we will only reply with the auth option
            response.setValue("auth", options.getAuthenticationStatus().getReason());
        } else {
            if (options.getAuthenticationStatus().isAuthenticated()) {
                response.setValue("auth", "ok");
            }
            boolean isTriggerTrace = options.getOptionValue(XTraceOption.TRIGGER_TRACE);
            if (isTriggerTrace) {
                if (!isServiceRoot) { //a continued trace, trigger trace flag has no effect
                    response.setValue("trigger-trace", "ignored");
                } else if (traceDecision.isSampled()) {
                    response.setValue("trigger-trace", "ok");
                } else if (traceDecision.getTraceConfig() == null) {
                    response.setValue("trigger-trace", "settings-not-available");
                } else if (traceDecision.getTraceConfig().getFlags() == TracingMode.DISABLED.toFlags()) {
                    response.setValue("trigger-trace", "tracing-disabled");
                } else if (!traceDecision.getTraceConfig().hasSampleTriggerTraceFlag()) {
                    response.setValue("trigger-trace", "trigger-tracing-disabled");
                } else if (traceDecision.isBucketExhausted()) {
                    response.setValue("trigger-trace", "rate-exceeded");
                } else {
                    response.setValue("trigger-trace", "unknown-failure");
                }
            } else {
                response.setValue("trigger-trace", "not-requested");
            }

            for (XTraceOptions.XTraceOptionException exception : options.getExceptions()) {
                exception.appendToResponse(response);
            }
        }

        return response;
    }
    
    private final Map<String, String> keyValues = new LinkedHashMap<String, String>();
    
    private XTraceOptionsResponse() {
        
    }
    
    public String getValue(String key) {
        return keyValues.get(key);
    }
    
    public void setValue(String key, String value) {
        keyValues.put(key, value);
    }
    
    
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (Entry<String, String> entry : keyValues.entrySet()) {
            builder.append(entry.getKey() + "=" + entry.getValue() + ";");
        }
        
        if (builder.length() > 0) {
            builder.deleteCharAt(builder.length() - 1); //remove the last dangling ;
        }
        return builder.toString();
    }
    
    
}
