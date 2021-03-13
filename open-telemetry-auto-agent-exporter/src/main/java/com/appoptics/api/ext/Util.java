package com.appoptics.api.ext;

import com.tracelytics.ext.google.common.base.Strings;
import com.tracelytics.joboe.Constants;
import io.opentelemetry.api.trace.SpanContext;

public class Util {
    static String buildXTraceId(SpanContext context) {
        return buildXTraceId(context.getTraceId(), context.getSpanId(), context.isSampled());
    }

    static String buildXTraceId(String traceId, String spanId, boolean isSampled) {
        final String HEADER = "2B";
        String hexString = HEADER +
                Strings.padEnd(traceId, Constants.MAX_TASK_ID_LEN * 2, '0') +
                Strings.padEnd(spanId, Constants.MAX_OP_ID_LEN * 2, '0');
        hexString += isSampled ? "01" : "00";


        return hexString.toUpperCase();
    }
}
