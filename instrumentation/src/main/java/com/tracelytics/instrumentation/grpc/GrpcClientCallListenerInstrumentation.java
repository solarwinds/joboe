package com.tracelytics.instrumentation.grpc;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.MethodMatcher;
import com.tracelytics.instrumentation.SpanAware;
import com.tracelytics.instrumentation.http.ServletInstrumentation;
import com.tracelytics.joboe.span.impl.Span;
import com.tracelytics.joboe.span.impl.Span.SpanProperty;

/**
 * Instruments `io.grpc.ClientCall$Listener` to close the grpc client span at method `onClose`
 * @author Patson
 *
 */
public class GrpcClientCallListenerInstrumentation extends ClassInstrumentation {
    private static final String CLASS_NAME = GrpcClientCallListenerInstrumentation.class.getName();
    

    // List of method matchers that declare method with signature patterns that we want to instrument
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<OpType>> methodMatchers = Arrays
            .asList(new MethodMatcher<OpType>("onClose", new String[] { "io.grpc.Status", "io.grpc.Metadata" }, "void", OpType.ON_CLOSE));

    private enum OpType {
        ON_CLOSE;
    }

    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes) throws Exception {

        for (Entry<CtMethod, OpType> methodEntry : findMatchingMethods(cc, methodMatchers).entrySet()) {
            CtMethod method = methodEntry.getKey();
            OpType type = methodEntry.getValue();
            
            if (type == OpType.ON_CLOSE) {
                insertBefore(method, CLASS_NAME + ".onClose(this, $2, $1.getCode().toString() , $1.getCause(), $1.getDescription(),  $1.getCode() == io.grpc.Status.Code.INTERNAL || $1.getCode() == io.grpc.Status.Code.UNKNOWN);", false);
            } else {
                logger.warn("Unhandled type for " + CLASS_NAME + " type " + type);
            }
        }

        addSpanAware(cc);
        return true;
    }

    public static void onClose(Object listenerObject, Object headersObject, String statusCode, Throwable throwable, String description, boolean isError) {
        if (!(listenerObject instanceof SpanAware)) {
            logger.warn("Cannot end gRPC span as listener was not tagged properly");
            return;
        }
        
        SpanAware listener = (SpanAware) listenerObject;

        if (listener.tvGetSpan() == null) { // already exited or no span was created
            return;
        }
        
        Span span = (Span) listener.tvGetSpan();
        
        if (headersObject instanceof GrpcMetadata) {
            if (span.context().isSampled()) {
                String responseXTrace = (String) ((GrpcMetadata) headersObject).tvGet(ServletInstrumentation.XTRACE_HEADER);
                if (responseXTrace != null) {
                    span.setSpanPropertyValue(SpanProperty.CHILD_EDGES, Collections.singleton(responseXTrace));
                }
            }
        } else {
            logger.warn("Cannot extract response x-trace ID as metadata was not tagged properly");
        }
        
        if (statusCode != null) {
            span.setTag("GRPCStatus", statusCode);
        }
        
        if (isError) {
            if (throwable != null) {
                reportError(span, throwable, description != null ? description : statusCode);
            } else {
                reportError(span, statusCode, description != null ? description : statusCode);
            }
        }
        
        span.finish();
        
        listener.tvSetSpan(null); //clean up
    }
}