package com.tracelytics.instrumentation.grpc;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtConstructor;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.ext.javassist.NotFoundException;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.ConstructorMatcher;
import com.tracelytics.instrumentation.MethodMatcher;
import com.tracelytics.instrumentation.SpanAware;
import com.tracelytics.instrumentation.http.ServletInstrumentation;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.XTraceHeader;
import com.tracelytics.joboe.span.impl.ScopeManager;
import com.tracelytics.joboe.span.impl.Span;
import com.tracelytics.joboe.span.impl.Span.SpanProperty;
import com.tracelytics.joboe.span.impl.Span.TraceProperty;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;

/**
 * Instruments `io.grpc.internal.ServerCallImpl` to start and finish "grpc-server" span
 * 
 * On constructor, the "grpc-server' span is started; on call to `close` the "grpc-server" span is ended
 * 
 * Take note this acts as an entry point of a trace, therefore all the general logic of starting a trace applies to this class 
 * 
 * @author Patson
 *
 */
public class GrpcServerCallInstrumentation extends ClassInstrumentation {
    private static final String CLASS_NAME = GrpcServerCallInstrumentation.class.getName();
    private static final String LAYER_NAME = "grpc-server";
    
    // List of method matchers that declare method with signature patterns that we want to instrument
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<OpType>> methodMatchers = Arrays
            .asList(new MethodMatcher<OpType>("close", new String[] { "io.grpc.Status", "io.grpc.Metadata" }, "void", OpType.CLOSE));
    
    
    @SuppressWarnings("unchecked")
    private static List<ConstructorMatcher<OpType>> constructorMatchers = Arrays.asList(
            new ConstructorMatcher<OpType>(new String[] { "io.grpc.internal.ServerStream", "io.grpc.MethodDescriptor", "io.grpc.Metadata"}, OpType.CTOR)
    );
    
    
    private enum OpType {
        CLOSE, CTOR;
    }

    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {
        CtClass serverStreamClass = classPool.get("io.grpc.internal.ServerStream");
        String getAttributesMethodName;
         
        try {
            serverStreamClass.getMethod("getAttributes", "()Lio/grpc/Attributes;");
            getAttributesMethodName = "getAttributes";
        } catch (NotFoundException e) {
            try {
                serverStreamClass.getMethod("attributes",  "()Lio/grpc/Attributes;");
                getAttributesMethodName = "attributes";
            } catch (NotFoundException e1) {
                logger.info("Cannot apply instrumentation for " + CLASS_NAME + ", cannot find attributes method. Probably not a supported gRPC version" );
                return false;
            }
        }
        
        boolean hasAuthorityMethod;
        try {
            serverStreamClass.getMethod("getAuthority", "()Ljava/lang/String;");
            hasAuthorityMethod = true;
        } catch (NotFoundException e) {
            hasAuthorityMethod = false;
        }
        
        
        for (CtConstructor constructor : findMatchingConstructors(cc, constructorMatchers).keySet()) {
            insertAfter(constructor,
                    "String remoteAddress = null;"
                  + "String fullMethodName = null;"
                  + "String methodType = null;"
                  + "String authority = null;"
                  + "if ($1 != null) { "
                  + "    java.util.Iterator iterator = $1." + getAttributesMethodName + "().keys().iterator();"
                  + "    while (iterator.hasNext()) {" //cannot use get directly as the equals method is not overridden in io.grpc.Attributes.Key
                  + "        io.grpc.Attributes.Key key = (io.grpc.Attributes.Key) iterator.next();" 
                  + "        if (\"remote-addr\".equals(key.toString())) {"
                  + "            Object remoteAddressValue = $1." + getAttributesMethodName + "().get(key);"
                  + "            if (remoteAddressValue != null) {"
                  + "                remoteAddress = remoteAddressValue.toString();"
                  + "            }"
                  + "            break;"
                  + "        }"
                  + "    }"
                  +  (hasAuthorityMethod ? "authority = $1.getAuthority();" : "")
                  + "}"
                  + "if ($2 != null) { "
                  + "    fullMethodName = $2.getFullMethodName(); "
                  + "    methodType = $2.getType().toString();"
                  + "}"
                  + CLASS_NAME + ".postConstructor(this, $3, authority, remoteAddress, fullMethodName, methodType);"
                  + "", true, false);
        }
        
        for (Entry<CtMethod, OpType> methodEntry : findMatchingMethods(cc, methodMatchers).entrySet()) {
            CtMethod method = methodEntry.getKey();
            OpType type = methodEntry.getValue();
            
            if (type == OpType.CLOSE) {
                insertBefore(method, CLASS_NAME + ".preClose(this, $2, $1.getCode().toString(), $1.getCause(), $1.getDescription(), $1.getCode() == io.grpc.Status.Code.INTERNAL || $1.getCode() == io.grpc.Status.Code.UNKNOWN);", false);
            }
        }
        
        addSpanAware(cc);

        return true;
    }
    
    public static void postConstructor(Object serverCallObject, Object metadataObject, String authority, String remoteAddress, String fullMethodName, String methodType) {
        if (!(metadataObject instanceof GrpcMetadata)) {
            logger.warn("Cannot extract x-trace ID span as metadata was not tagged properly");
        }
        
        SpanAware serverCall = (SpanAware) serverCallObject;
        if (serverCall.tvGetSpan() != null) { //already started
            return;
        }
        
        Context.clearMetadata();
        ScopeManager.INSTANCE.removeAllScopes();
        
        final GrpcMetadata metadata = (GrpcMetadata) metadataObject;
        String incomingXTrace = (String) metadata.tvGet(ServletInstrumentation.XTRACE_HEADER);
        
        //Attempts to start/continue trace as this is an entry point
        Span span = startTraceAsSpan(LAYER_NAME, incomingXTrace != null ? Collections.singletonMap(XTraceHeader.TRACE_ID, incomingXTrace) : Collections.<XTraceHeader, String>emptyMap(), fullMethodName, false);
    
        ClassInstrumentation.setForwardedTags(span, new HeaderExtractor<String, String>() {
            @Override public String extract(String s) {
                return (String)metadata.tvGet(s);
            }
        });
        
        if (fullMethodName != null) {
            span.setTag("URL", fullMethodName);
            
            int separatorIndex = fullMethodName.lastIndexOf('/');
            
            String controller;
            String action;
            if (separatorIndex != -1) {
                controller = fullMethodName.substring(0, separatorIndex);
                action = fullMethodName.substring(separatorIndex + 1);
            } else {
                controller = fullMethodName;
                action = null;
            }
            
            if (controller != null) {
                span.setTag("Controller", controller);
                span.setTracePropertyValue(TraceProperty.CONTROLLER, controller);
            }
            if (action != null) {
                span.setTag("Action", action);
                span.setTracePropertyValue(TraceProperty.ACTION, action);
            }
        } 
        
        if (methodType != null) {
            span.setTag("GRPCMethodType", methodType);
        }
        
        if (authority == null) { //try to extract from Metadata directly
            authority = (String) metadata.tvGet(":authority");
        }
        
        if (authority != null) {
            span.setTag("HTTP-Host", authority);
        }
        
        if (remoteAddress != null) {
            span.setTag("ClientIP", remoteAddress);
        }
        
        serverCall.tvSetSpan(span); //set to current object so ServerCall can end this properly
    }
    
    public static void preClose(Object serverCallObject, Object headersObject, String statusCode, Throwable throwable, String description, boolean isError) {
        SpanAware serverCall = (SpanAware) serverCallObject;
        Span span = (Span) serverCall.tvGetSpan();
        
        
        if (span != null) {
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
            
            if (span.getSpanPropertyValue(SpanProperty.EXIT_XID) != null) {
                if (!(headersObject instanceof GrpcMetadata)) {
                    logger.warn("Cannot tag response x-trace ID as metadata was not tagged properly");
                } else {
                    GrpcMetadata headers = (GrpcMetadata) headersObject;
                    headers.tvPut(ServletInstrumentation.XTRACE_HEADER, span.getSpanPropertyValue(SpanProperty.EXIT_XID));
                }
            }
            
            serverCall.tvSetSpan(null); //cleanup
        } 
    }
}