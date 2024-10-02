package com.tracelytics.instrumentation.http.akka.server;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.http.ServletInstrumentation;
import com.tracelytics.joboe.span.impl.Span;
import com.tracelytics.joboe.span.impl.SpanDictionary;

/**
 * Instruments the "default" exception handler declared in scala Object `akka.http.scaladsl.server.ExceptionHandler`
 */
public class AkkaHttpDefaultExceptionHandlerInstrumentation extends ClassInstrumentation {
    private static String CLASS_NAME = AkkaHttpDefaultExceptionHandlerInstrumentation.class.getName();

    @Override
    protected boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes) throws Exception {
        CtClass throwableType = classPool.get(Throwable.class.getName());
        CtClass requestContextType = classPool.get("akka.http.scaladsl.server.RequestContext");

        for (CtMethod method : cc.getDeclaredMethods()) {
            if (method.getName().startsWith("$anonfun$applyOrElse$")) { //instruments the partial function handling method
                CtClass[] parameterTypes = method.getParameterTypes();
                if (parameterTypes.length == 2) {
                    if (parameterTypes[0].subtypeOf(throwableType) && parameterTypes[1].subtypeOf(requestContextType)) { //ensure it's the case that handles `Throwable`
                        insertBefore(method, CLASS_NAME + ".beforeApplyException($1, $2.request());", false);
                    }
                }
            }

        }

        return true;
    }

    public static void beforeApplyException(Throwable throwable, Object requestObject) {
        if (throwable != null && requestObject instanceof AkkaHttpRequest) {
            String spanId = ((AkkaHttpRequest) requestObject).tvGetHeader(ServletInstrumentation.X_SPAN_KEY);
            Span span = SpanDictionary.getSpan(Long.valueOf(spanId));
            if (span != null) {
                reportError(span, throwable);
            }
        }
    }
}
