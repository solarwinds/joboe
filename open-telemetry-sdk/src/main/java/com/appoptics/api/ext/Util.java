package com.appoptics.api.ext;

import com.tracelytics.ext.google.common.base.Strings;
import com.tracelytics.joboe.Constants;
import io.opentelemetry.trace.SpanContext;

public class Util {
    private final static String HEADER = "2B";
    public static String buildXTraceId(SpanContext otSpanContext) {
        return buildXTraceId(otSpanContext.getTraceId().toLowerBase16(), otSpanContext.getSpanId().toLowerBase16(), otSpanContext.getTraceFlags().isSampled());
    }

    public static String buildXTraceId(String traceId, String spanId, boolean isSampled) {
        String hexString = HEADER +
                Strings.padEnd(traceId, Constants.MAX_TASK_ID_LEN * 2, '0') +
                Strings.padEnd(spanId, Constants.MAX_OP_ID_LEN * 2, '0');
        hexString += isSampled ? "01" : "00";


        return hexString.toUpperCase();
    }
}
