package com.appoptics.api.ext;

import com.tracelytics.ext.google.common.base.Strings;
import com.tracelytics.joboe.Constants;
import com.tracelytics.joboe.Metadata;
import com.tracelytics.joboe.OboeException;
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

    static Metadata buildMetadata(SpanContext context) {
        try {
            Metadata metadata = new Metadata(buildXTraceId(context));
            metadata.setTraceId(buildTraceId(context.getTraceIdBytes()));
            return metadata;
        } catch (OboeException e) {
            return null;
        }
    }

    static Long buildTraceId(byte[] traceIdBytes) {
        long value = 0;
        int length = Math.min(8, traceIdBytes.length);
        for (int i = 0; i < length; i++) {
            value += ((long) traceIdBytes[i] & 0xffL) << (8 * i);
        }
        return value;
    }
}
