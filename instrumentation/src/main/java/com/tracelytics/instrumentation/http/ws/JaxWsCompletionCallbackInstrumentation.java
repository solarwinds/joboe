package com.tracelytics.instrumentation.http.ws;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.tracelytics.ext.javassist.CannotCompileException;
import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtField;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.ext.javassist.CtNewMethod;
import com.tracelytics.ext.javassist.NotFoundException;
import com.tracelytics.ext.javassist.expr.ExprEditor;
import com.tracelytics.ext.javassist.expr.MethodCall;
import com.tracelytics.instrumentation.MethodSignature;
import com.tracelytics.instrumentation.http.ServletInstrumentation;

/**
 * Instrumentation on the callback using the jax-ws RI (reference implementation). Take note that the jax-ws RI is included in most JVM with package name
 * com.xml.internal.ws while the downloadable version from GlassFish jax-ws RI uses package name com.xml.ws
 * 
 * Other than the package name difference, the instrumentation logic is exactly the same for the "internal" and "non-internal" version
 * 
 * We attached the context when the callback was passed to the Client (instrumented in JaxWsClientInstrumentation) and then when the onCompletion method is called,
 * we trace it as the exit event. Take note that we cannot directly insert the exit code as insertAfter as the Http Response headers (which might have x-trace id from
 * the SOAP server) can only be found in outer class field responseImpl type AsyncResponseImpl. Therefore we instead insert code after the responseImpl is referenced
 * for the "set()" invocation. "set()" method indicates successful completion of the FutureTask which the AsyncResponseImpl extends.
 * 
 * 
 * @author Patson Luk
 * @see <a href="http://jax-ws.java.net/">GlassFish/Sun Metro JAX-WS RI</a>
 */
public class JaxWsCompletionCallbackInstrumentation extends BaseWsClientCallbackInstrumentation {
    private static final String CLASS_NAME = JaxWsCompletionCallbackInstrumentation.class.getName();
    private static final String LAYER_NAME = "soap_client_jax-ws";
    
    private static final String CONTEXT_FIELD = "tvContext";
    
    private static MethodSignature[] onCompletionMethods = {
        new MethodSignature("onCompletion", "(Lcom/sun/xml/internal/ws/api/message/Packet;)V"),
        new MethodSignature("onCompletion", "(Lcom/sun/xml/ws/api/message/Packet;)V"),
        new MethodSignature("onCompletion", "(Ljava/lang/Throwable;)V"),
                                                     };
    
    @Override
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {
        
        //insert extra field, setter, getter and tag our interface to it
        addTvContextObjectAware(cc);
        
        List<CtMethod> methods = findMethods(cc, onCompletionMethods);
        
        
        for (CtMethod method : methods) {
            if (shouldModify(cc, method)) {
                method.instrument(new ExprEditor() {

                    public void edit(MethodCall m) throws CannotCompileException {
                        if (m.getMethodName().equals("set")) {
                            insertAfterMethodCall(m, CLASS_NAME + ".layerExit(this, $0.getContext() != null ? (java.util.Map)$0.getContext().get(javax.xml.ws.handler.MessageContext.HTTP_RESPONSE_HEADERS) : null);", false);
                        }
                    }
                });
            }
        }

        
        return true;
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
    
    public static void layerExit(Object callback, Map<String, ?> headers) {
        List<String> xTraceIds = (List<String>) ((headers != null) ? headers.get(ServletInstrumentation.XTRACE_HEADER) : null);
        String xTraceId = (xTraceIds != null && !xTraceIds.isEmpty()) ? xTraceIds.get(0) : null;
        layerExitAsync(LAYER_NAME, callback, xTraceId);        
    }
}
