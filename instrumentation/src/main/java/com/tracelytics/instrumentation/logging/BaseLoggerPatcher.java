package com.tracelytics.instrumentation.logging;

import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.Metadata;
import com.tracelytics.joboe.config.LogTraceIdScope;

/**
 * Base class for patching Loggers to include trace context
 * @author pluk
 *
 */
public abstract class BaseLoggerPatcher extends ClassInstrumentation {
    protected static final String TRACE_ID_KEY = "ao.traceId";
    
    protected static String getLogTraceId(LogTraceIdScope scope) {
        Metadata metadata = Context.getMetadata();
        if ((scope == LogTraceIdScope.ENABLED && metadata.isValid()) || (scope == LogTraceIdScope.SAMPLED_ONLY && metadata.isSampled())) {
            return metadata.getCompactTraceId();
        } else {
            return null;
        }
    }
}

    