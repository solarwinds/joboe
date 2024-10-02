package com.tracelytics.instrumentation.grpc;

import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

import com.tracelytics.ext.javassist.CannotCompileException;
import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtConstructor;
import com.tracelytics.ext.javassist.CtField;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.ext.javassist.NotFoundException;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.ConstructorMatcher;
import com.tracelytics.instrumentation.MethodMatcher;
import com.tracelytics.instrumentation.SpanAware;
import com.tracelytics.joboe.span.impl.Span;
import com.tracelytics.joboe.span.impl.Span.TraceProperty;

/**
 * Instruments `io.grpc.internal.ServerCallImpl$ServerStreamListenerImpl` such that on abrupt call interruption caused by client,
 * server gRPC span would still exit properly
 * 
 * Take note that exit via this route will not have x-trace id injected in the response Metadata
 * 
 * @author pluk
 *
 */
public class GrpcServerStreamListenerInstrumentation extends ClassInstrumentation {
    private static final String CLASS_NAME = GrpcServerStreamListenerInstrumentation.class.getName();
    
    // List of method matchers that declare method with signature patterns that we want to instrument
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<OpType>> methodMatchers = Arrays
            .asList(new MethodMatcher<OpType>("closed", new String[] { "io.grpc.Status" }, "void", OpType.CLOSED),
                    new MethodMatcher<OpType>("halfClosed", new String[] { }, "void", OpType.HALF_CLOSED),
                    new MethodMatcher<OpType>("messagesAvailable", new String[] { }, "void", OpType.MESSAGES_AVAILABLE),
                    new MethodMatcher<OpType>("onReady", new String[] { }, "void", OpType.ON_READY)
                    );
    
    
    @SuppressWarnings("unchecked")
    private static List<ConstructorMatcher<OpType>> constructorMatchers = Arrays.asList(
            new ConstructorMatcher<OpType>(new String[] { "io.grpc.internal.ServerCallImpl"}, OpType.CTOR)
    );
    
    
    private enum OpType {
        CLOSED, HALF_CLOSED, MESSAGES_AVAILABLE, ON_READY, CTOR;
    }

    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {
        cc.addField(CtField.make("private Object tvCall;", cc));
        
        
        for (CtConstructor constructor : findMatchingConstructors(cc, constructorMatchers).keySet()) {
            insertAfter(constructor, "this.tvCall = $1;", true, false);
        }
        
        for (Entry<CtMethod, OpType> methodEntry : findMatchingMethods(cc, methodMatchers).entrySet()) {
            CtMethod method = methodEntry.getKey();
            OpType type = methodEntry.getValue();
            
            if (type == OpType.CLOSED) {
                insertBefore(method, CLASS_NAME + ".preClosed(this.tvCall, $1.getCode().toString(), $1.getCause(), $1.getDescription(), $1.getCode() == io.grpc.Status.Code.INTERNAL || $1.getCode() == io.grpc.Status.Code.UNKNOWN);", false);
            } else if (type == OpType.HALF_CLOSED || type == OpType.MESSAGES_AVAILABLE || type == OpType.ON_READY) {
                addExceptionReporting(method);
            }
        }
        
        addSpanAware(cc);

        return true;
    }
    
   
    private void addExceptionReporting(CtMethod method) throws CannotCompileException, NotFoundException {
        method.addCatch("{ " + CLASS_NAME + ".onUnhandledException(this.tvCall, $e); throw $e; }", classPool.get("java.lang.Throwable"));
    }

    public static void onUnhandledException(Object serverCallObject, Throwable e) {
        if (!(serverCallObject instanceof SpanAware)) {
            logger.warn("gRPC server call object not patched properly");
            return;
        }
        
        SpanAware serverCall = (SpanAware) serverCallObject;
        Span span = (Span) serverCall.tvGetSpan();
        
        if (span != null && span.context().isSampled()) {
            span.setTag("GRPCStatus", "UNKNOWN");
            reportError(span, e, e.getMessage());
            //flag error for this trace
            span.setTracePropertyValue(TraceProperty.HAS_ERROR, true);
            span.finish();
            
            serverCall.tvSetSpan(null); //cleanup
        } 
       
        
    }

    public static void preClosed(Object serverCallObject, String statusCode, Throwable throwable, String description, boolean isError) {
        if (!(serverCallObject instanceof SpanAware)) {
            logger.warn("gRPC server call object not patched properly");
            return;
        }
        
        SpanAware serverCall = (SpanAware) serverCallObject;
        Span span = (Span) serverCall.tvGetSpan();
        
        
        if (span != null && span.context().isSampled()) {
            if (statusCode != null) {
                span.setTag("GRPCStatus", statusCode);
            }

            if (isError) {
                if (throwable != null) {
                    reportError(span, throwable, description != null ? description : statusCode);
                } else {
                    reportError(span, statusCode, description != null ? description : statusCode);
                }
                //flag error for this trace
                span.setTracePropertyValue(TraceProperty.HAS_ERROR, true);
            }
            span.finish();
            
            serverCall.tvSetSpan(null); //cleanup
        } 
    }
}