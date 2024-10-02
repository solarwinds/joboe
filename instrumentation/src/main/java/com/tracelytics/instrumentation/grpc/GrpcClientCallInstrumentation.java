package com.tracelytics.instrumentation.grpc;

import java.util.Arrays;
import java.util.List;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtConstructor;
import com.tracelytics.ext.javassist.CtField;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.ext.javassist.CtNewMethod;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.MethodMatcher;
import com.tracelytics.instrumentation.SpanAware;
import com.tracelytics.instrumentation.http.ServletInstrumentation;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.Metadata;
import com.tracelytics.joboe.span.impl.Span;
import com.tracelytics.joboe.span.impl.Span.SpanProperty;
import com.tracelytics.joboe.span.impl.Tracer;
import com.tracelytics.joboe.span.impl.Tracer.SpanBuilder;
import com.tracelytics.joboe.span.impl.TraceEventSpanReporter;

/**
 * Instruments `io.grpc.internal.ClientCallImpl` to create grpc-client span
 * 
 * A span is starts on `start` method, take note that it ends on a different instrumentation {@link GrpcClientCallListenerInstrumentation}.
 * 
 * The class is also patched to exposed extra information via {@link GrpcClientCall}.  
 * 
 * @author Patson
 *
 */
public class GrpcClientCallInstrumentation extends ClassInstrumentation {
    private static final String CLASS_NAME = GrpcClientCallInstrumentation.class.getName();
    private static final String LAYER_NAME = "grpc-client";
    
    // List of method matchers that declare method with signature patterns that we want to instrument
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<OpType>> methodMatchers = Arrays.asList(
         new MethodMatcher<OpType>("start", new String[] { "io.grpc.ClientCall$Listener", "io.grpc.Metadata" }, "void", OpType.START)
    );
    
    private enum OpType {
        START;
    }

    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {

        cc.addField(CtField.make("private String tvMethod;", cc));
        cc.addMethod(CtNewMethod.make("public String tvGetMethod() { return this.tvMethod; }", cc));
        
        cc.addField(CtField.make("private String tvMethodType;", cc));
        cc.addMethod(CtNewMethod.make("public String tvGetMethodType() { return this.tvMethodType; }", cc));
        
        for (CtConstructor constructor : cc.getConstructors()) {
            if (constructor.getParameterTypes().length >=1 && constructor.getParameterTypes()[0].getName().equals("io.grpc.MethodDescriptor")) {
                insertBefore(constructor, 
                        "if ($1 != null) { "
                      + "    this.tvMethod = $1.getFullMethodName(); "
                      + "    this.tvMethodType = $1.getType().toString(); "
                      + "}");
            }
        }
        
        cc.addField(CtField.make("private String tvTarget;", cc));
        cc.addMethod(CtNewMethod.make("public void tvSetTarget(String target) { this.tvTarget = target; }", cc));
        cc.addMethod(CtNewMethod.make("public String tvGetTarget() { return this.tvTarget; }", cc));
        
        for (CtMethod startMethod : findMatchingMethods(cc, methodMatchers).keySet()) {
            insertBefore(startMethod, CLASS_NAME + ".start($1, $2, this);", false);
        }
        
        tagInterface(cc, GrpcClientCall.class.getName());

        return true;
    }
    
    public static void start(Object listenerObject, Object metadataObject, Object clientCallObject) {
        if (!(listenerObject instanceof SpanAware)) {
            logger.warn("Cannot start gRPC span as listener was not tagged properly");
            return;
        } else if (!(metadataObject instanceof GrpcMetadata)) {
            logger.warn("Cannot start gRPC span as metadata was not tagged properly");
            return;
        }
        
        
        GrpcMetadata metadata = (GrpcMetadata) metadataObject;
        
        if (Context.getMetadata().isSampled()) {
            GrpcClientCall clientCall = (GrpcClientCall) clientCallObject;
            SpanAware listener = (SpanAware) listenerObject;
            
            if (listener.tvGetSpan() != null) { //already started
                return;
            }
            
            SpanBuilder spanBuilder = Tracer.INSTANCE.buildSpan(LAYER_NAME).withReporters(TraceEventSpanReporter.REPORTER);
            
            String remoteUrl = "grpc://";
            
            String target = clientCall.tvGetTarget();
            if (target != null) {
                remoteUrl += target + "/";
            } else {
                logger.warn("Failed to locate target for grpc call");
            }
            
            String method = clientCall.tvGetMethod();
            if (method != null) {
                remoteUrl += method;
            } else {
                logger.warn("Failed to locate method for grpc call");
            }
            
            spanBuilder.withTag("RemoteURL", remoteUrl);
            
            String methodType = clientCall.tvGetMethodType();
            if (methodType != null) {
                spanBuilder.withTag("GRPCMethodType", methodType);
            }
            
            spanBuilder.withTag("IsService", true);
            spanBuilder.withTag("Spec", "rsc");
            
            boolean isAsync = GrpcClientCallsPatcher.isAsync(); 
            
            Metadata existingContext = null;
            if (isAsync) {
                spanBuilder.withSpanProperty(SpanProperty.IS_ASYNC, true);
                existingContext = Context.getMetadata();
            }            
            
            Span span = spanBuilder.start();
            listener.tvSetSpan(span); //set to listener so it can finishes the span
            
            metadata.tvPut(ServletInstrumentation.XTRACE_HEADER, span.context().getMetadata().toHexString());
            
            //reset context back to what it was if this call is async
            if (isAsync) {
                Context.setMetadata(existingContext);
            }
        } else if (Context.getMetadata().isValid()) { //inject x-trace header even if it's not sampled
            metadata.tvPut(ServletInstrumentation.XTRACE_HEADER, Context.getMetadata().toHexString());
            //might need to create a span in the future if we capture outbound metrics
        }
    }

}