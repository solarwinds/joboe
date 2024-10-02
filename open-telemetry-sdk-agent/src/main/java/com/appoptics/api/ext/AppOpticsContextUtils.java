package com.appoptics.api.ext;

import com.tracelytics.ext.google.common.base.Strings;
import com.tracelytics.instrumentation.HeaderConstants;
import com.tracelytics.joboe.Constants;
import com.tracelytics.joboe.Metadata;
import com.tracelytics.joboe.OboeException;
import com.tracelytics.joboe.span.impl.ScopeManager;
import io.grpc.Context;
import io.opentelemetry.context.ContextUtils;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.trace.*;

import java.util.logging.Logger;

public class AppOpticsContextUtils {
//    private static WeakHashMap<TraceId, Metadata> contextMetadataLookup = new WeakHashMap<TraceId, Metadata>();
    private static Logger logger = Logger.getLogger("agent-sdk");

    private static ContextChangeListener contextChangeListener = AppOpticsContextChangeListener.INSTANCE;

//    static void setContextMetadata(TraceId otTraceId, Metadata contextMetadata) {
//        contextMetadataLookup.put(otTraceId, new Metadata(contextMetadata)); //create a clone, otherwise the metadata might get invalidated before it's used
//    }

    //remove method?? probably does not know when to remove

    static Metadata buildAoMetadata(SpanContext spanContext) {
        return buildAoMetadata(spanContext.getTraceState(), spanContext.getTraceId(), spanContext.getSpanId(), spanContext.getTraceFlags());
    }

//    static Metadata buildAoMetadata(SpanContext spanContext, SpanId spanIdOverride) {
//        return buildAoMetadata(spanContext.getTraceState(), spanContext.getTraceId(), spanIdOverride, spanContext.getTraceFlags());
//    }

    static Metadata buildAoMetadata(SpanData spanData) {
        return buildAoMetadata(spanData.getTraceState(), spanData.getTraceId(), spanData.getSpanId(), spanData.getTraceFlags());
    }


    static Metadata buildAoMetadata(TraceState traceState, TraceId traceId, SpanId spanId, TraceFlags flags) {


        Metadata parentMetadata = null;

        com.tracelytics.joboe.span.impl.Scope currentAoScope = ScopeManager.INSTANCE.active();
        if (currentAoScope != null && currentAoScope.span().context().getMetadata().isValid()) {
            parentMetadata = currentAoScope.span().context().getMetadata();
        } else if (traceState.get(HeaderConstants.XTRACE_HEADER.toLowerCase()) != null) { //use x-trace header to find out the longer AO task ID
            try {
                parentMetadata = new Metadata(traceState.get(HeaderConstants.XTRACE_HEADER.toLowerCase()));
            } catch (OboeException e) {
                e.printStackTrace();
            }
        }
//        if (parentMetadata == null) { //then try the in-process lookup
//            parentMetadata = contextMetadataLookup.get(traceId);
//        }

        if (parentMetadata == null) { //Then create AO trace from padding zeroes
            logger.fine("Cannot find AO context for OT trace id " + traceId);
            try {
                return new Metadata(buildXTraceId(traceId.toLowerBase16(), spanId.toLowerBase16(), flags.isSampled()));
            } catch (OboeException e) {
                e.printStackTrace();
                return null;
            }
        } else { //already a ao context for this trace ID
            Metadata result = new Metadata(parentMetadata); //use a clone so modification would not affect the original copy
            String opId = spanId.toLowerBase16().toUpperCase();
            try {
                result.setOpID(opId);
            } catch (OboeException e) {
                e.printStackTrace();
            }

            result.setSampled(flags.isSampled());

            return result;
        }
    }

    private final static String HEADER = "2B";

    public static String buildXTraceId(String traceId, String spanId, boolean isSampled) {
        String hexString = HEADER +
                Strings.padEnd(traceId, Constants.MAX_TASK_ID_LEN * 2, '0') +
                Strings.padEnd(spanId, Constants.MAX_OP_ID_LEN * 2, '0');
        hexString += isSampled ? "01" : "00";


        return hexString.toUpperCase();
    }

    //workaround...since there's no listener available yet : https://github.com/open-telemetry/opentelemetry-java/issues/922
    public static Scope withScopedContext(Context context) {
        Context oldContext = Context.current();
        Scope wrappedScope = ContextUtils.withScopedContext(context); //wrap scope so we catch the scope.close()
        contextChangeListener.onContextChange(oldContext, context);
        return wrappedScope;
    }
}
