package com.tracelytics.instrumentation.http.ws;

import java.util.ArrayList;
import java.util.List;

import com.tracelytics.ext.javassist.CannotCompileException;
import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtField;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.ext.javassist.NotFoundException;
import com.tracelytics.instrumentation.MethodSignature;
import com.tracelytics.instrumentation.http.ServletInstrumentation;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.Metadata;

/**
 * Instrumentation on the client using the jax-ws RI (reference implementation). Take note that the jax-ws RI is included in most JVM with package name
 * com.xml.internal.ws while the downloadable version from GlassFish jax-ws RI uses package name com.xml.ws
 * 
 * Other than the package name difference, the instrumentation logic is exactly the same for the "internal" and "non-internal" version
 * 
 * @author Patson Luk
 * @see <a href="http://jax-ws.java.net/">GlassFish/Sun Metro JAX-WS RI</a>
 */
public class JaxWsClientInstrumentation extends BaseWsClientInstrumentation {
    private static final String CLASS_NAME = JaxWsClientInstrumentation.class.getName();
    private static final String LAYER_NAME = "soap_client_jax-ws";
    private static final String CONTEXT_FIELD_NAME = "tvContext";
    
    //methods with Packet and RequestContext as 1st and 2nd param
    private static MethodSignature[] doProcessMethods = {
        new MethodSignature("doProcess", "(Lcom/sun/xml/internal/ws/api/message/Packet;Lcom/sun/xml/internal/ws/client/RequestContext;Lcom/sun/xml/internal/ws/client/ResponseContextReceiver;)Lcom/sun/xml/internal/ws/api/message/Packet;"), //jax-ws RI
        new MethodSignature("doProcessAsync", "(Lcom/sun/xml/internal/ws/api/message/Packet;Lcom/sun/xml/internal/ws/client/RequestContext;Lcom/sun/xml/internal/ws/api/pipe/Fiber$CompletionCallback;)V"), //java-ws RI of java 7 and before
        new MethodSignature("doProcess", "(Lcom/sun/xml/ws/api/message/Packet;Lcom/sun/xml/ws/client/RequestContext;Lcom/sun/xml/ws/client/ResponseContextReceiver;)Lcom/sun/xml/ws/api/message/Packet;")//glassfish
    };
    
    
    //methods with Packet and RequestContext as 2nd and 3rd param
    private static MethodSignature[] doProcessMethods2 = {
        new MethodSignature("doProcessAsync", "(Lcom/sun/xml/internal/ws/client/AsyncResponseImpl;Lcom/sun/xml/internal/ws/api/message/Packet;Lcom/sun/xml/internal/ws/client/RequestContext;Lcom/sun/xml/internal/ws/api/pipe/Fiber$CompletionCallback;)V"), //java-ws RI of java 8
        new MethodSignature("doProcessAsync", "(Lcom/sun/xml/ws/client/AsyncResponseImpl;Lcom/sun/xml/ws/api/message/Packet;Lcom/sun/xml/ws/client/RequestContext;Lcom/sun/xml/ws/api/pipe/Fiber$CompletionCallback;)V") //glassfish
    };
                                                     
    
    @Override
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {
        
        cc.addField(CtField.make("private " + Metadata.class.getName() + " " + CONTEXT_FIELD_NAME + ";", cc));
        
        List<CtMethod> methods = findMethods(cc, doProcessMethods);
		
        for (CtMethod method : methods) {
            if (shouldModify(cc, method)) {
                boolean isAsync = "doProcessAsync".equals(method.getName());
                
                insertLayerEntryCode(method, "$1", "$2", isAsync);
                
                if (!isAsync) {
                    insertLayerExitCode(method);
                } else {
                    insertStoreContextCode(method, "$3");
                }
            }
        }
        
        methods = findMethods(cc, doProcessMethods2);
        
        for (CtMethod method : methods) {
            if (shouldModify(cc, method)) {
                boolean isAsync = "doProcessAsync".equals(method.getName());
                
                insertLayerEntryCode(method, "$2", "$3", isAsync);
                
                if (!isAsync) {
                    insertLayerExitCode(method);
                } else {
                    insertStoreContextCode(method, "$4");
                }
            }
        }
        
        
        return true;
    }
    
    private static void insertLayerEntryCode(CtMethod method, String paramPacketToken, String paramRequestContextToken, boolean isAsync) throws CannotCompileException {
        String code = "String tvSoapAction = " + paramPacketToken + " != null ? " + paramPacketToken + ".soapAction : null;" +
                "if (tvSoapAction == null || tvSoapAction.equals(\"\")) { " +
                "    if (" + paramRequestContextToken + " != null && " + paramRequestContextToken + ".getSoapAction() != null) {" +
                "        tvSoapAction = " + paramRequestContextToken + ".getSoapAction();" +
                "    }" +
                "} ";
        
        if (isAsync) {
            code += CONTEXT_FIELD_NAME + " = " + CLASS_NAME + ".layerEntrySoap(tvSoapAction, " +
                    "    (" + paramRequestContextToken + " != null && " + paramRequestContextToken + ".getEndpointAddress() != null) ? " + paramRequestContextToken + ".getEndpointAddress().toString() : null," +
                    "    \"" + LAYER_NAME + "\"," +
                    "    true);";
            //patch the context to the request header
            code += "if (" + paramRequestContextToken + " != null && " + Context.class.getName() + ".getMetadata().isValid()) { " +
                    "    java.util.Map requestHeaders = (java.util.Map)" + paramRequestContextToken + ".get(javax.xml.ws.handler.MessageContext.HTTP_REQUEST_HEADERS);" +
                    "     if (requestHeaders == null) {" +
                    "        requestHeaders = new java.util.HashMap();" +
                             paramRequestContextToken + ".put(javax.xml.ws.handler.MessageContext.HTTP_REQUEST_HEADERS, requestHeaders);" +
                    "     }" +
                    "     java.util.List tvXTraceId = (java.util.List)requestHeaders.get(\"" + ServletInstrumentation.XTRACE_HEADER + "\");" +
                    "     if (tvXTraceId == null) {" +
                    "        tvXTraceId = new java.util.ArrayList();" +
                    "        requestHeaders.put(\"" + ServletInstrumentation.XTRACE_HEADER + "\", tvXTraceId);" +
                    "     }" +
                    "     tvXTraceId.add(" + CONTEXT_FIELD_NAME + ".toHexString());" +
                    "}";
        } else {
            code += CLASS_NAME + ".layerEntrySoap(tvSoapAction, " +
                    "    (" + paramRequestContextToken + " != null && " + paramRequestContextToken + ".getEndpointAddress() != null) ? " + paramRequestContextToken + ".getEndpointAddress().toString() : null," +
                    "    \"" + LAYER_NAME + "\");";
            //do not have to patch the context to request header cause HttpURLConnectionPatcher would do it, and they are all in the same thread
        }
        
        insertBefore(method, code, false);
                
    }
    
    private static void insertLayerExitCode(CtMethod method) throws CannotCompileException {
        insertAfter(method, "java.util.Map responseHeaders = null;" +
                "if (this instanceof javax.xml.ws.BindingProvider && this.getResponseContext() != null) {" +
                "    responseHeaders = (java.util.Map)this.getResponseContext().get(javax.xml.ws.handler.MessageContext.HTTP_RESPONSE_HEADERS);" +
               " }" +
                "java.util.List xTraceId = (responseHeaders != null) ? responseHeaders.get(\"" + ServletInstrumentation.XTRACE_HEADER +  "\") : null;" +
                CLASS_NAME + ".layerExitSoap(\"" + LAYER_NAME + "\", xTraceId, false);");
    }
    
    private static void insertStoreContextCode(CtMethod method, String paramCallbackToken) throws CannotCompileException {
        insertAfter(method, CLASS_NAME + ".storeContext(" + paramCallbackToken + ", " + CONTEXT_FIELD_NAME + ");" +
                            CONTEXT_FIELD_NAME + " = null;");
    }
    
    
    
    protected static List<CtMethod> findMethods(CtClass clazz, MethodSignature...signatures) {
        List<CtMethod> methods = new ArrayList<CtMethod>();
        
        for (MethodSignature s: signatures) {
            try {
                methods.add(clazz.getMethod(s.getName(), s.getSignature())); 
            } catch(NotFoundException ex) {
                continue;
            }
        }
        return methods;
    }
}
