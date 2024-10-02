package com.tracelytics.instrumentation.http.ws.server;

import com.tracelytics.ext.javassist.CannotCompileException;
import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.ext.javassist.bytecode.annotation.Annotation;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.joboe.span.impl.ScopeManager;
import com.tracelytics.joboe.span.impl.Span;
import com.tracelytics.joboe.span.impl.Span.TraceProperty;
import com.tracelytics.instrumentation.AnnotationUtils;

/**
 * Instruments classes that are annotated with `javax.jws.WebService` or `javax.jws.WebServiceProvider`
 * 
 * Scans the declared methods for `javax.jws.WebMethod` annotations (either annotated directed to the method or in the overridden parent method)
 * 
 * @author pluk
 *
 */
public class JaxWsWebServiceInstrumentation extends ClassInstrumentation {
    private static final String CLASS_NAME = JaxWsWebServiceInstrumentation.class.getName();
    @Override
    protected boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes) throws Exception {
        boolean modified = false;
        for (CtMethod method : cc.getDeclaredMethods()) {
            if (shouldModify(cc, method)) {
                for (Annotation annotation : AnnotationUtils.getAnnotationsFromBehavior(method, true)) {
                    if (annotation.getTypeName().equals("javax.jws.WebMethod")) {
                        logger.debug("Instrumenting jax-ws web method " + method);
                        modifyWebMethod(method);
                        modified = true;
                        break;
                    }
                }
            }
        }
        return modified;
    }
   
    
    private void modifyWebMethod(CtMethod method) throws CannotCompileException {
        ClassInstrumentation.insertBefore(method, CLASS_NAME + ".setControllerAction(\"" + method.getDeclaringClass().getName() + "\", \"" + method.getName() + "\");", false);
    }
    
    public static void setControllerAction(String controller, String action) {
        Span currentSpan = ScopeManager.INSTANCE.activeSpan();
        if (currentSpan != null) {
            currentSpan.setTag("Controller", controller);
            currentSpan.setTracePropertyValue(TraceProperty.CONTROLLER, controller);
            currentSpan.setTag("Action", action);
            currentSpan.setTracePropertyValue(TraceProperty.ACTION, action);
        }
    }

   
}
