package com.tracelytics.instrumentation.http.akka.server;

import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

import com.tracelytics.ext.javassist.CannotCompileException;
import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.ext.javassist.expr.ExprEditor;
import com.tracelytics.ext.javassist.expr.MethodCall;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.MethodMatcher;
import com.tracelytics.instrumentation.http.ServletInstrumentation;
import com.tracelytics.joboe.span.impl.Span;
import com.tracelytics.joboe.span.impl.SpanDictionary;

/**
 * Instruments the `InHandler` of `PrepareRequests` stage of `HttpServerBluePrint` to extract the remote address from request header
 *
 * Reports such a header as KV `ClientIP`. Take note that this header is only available if `remote-address-header` is set to `on`
 * within the akka http config, such a value is default to `off`. See https://doc.akka.io/docs/akka-http/current/configuration.html
 *
 */
public class AkkaHttpPrepareRequestsInstrumentation extends ClassInstrumentation {

    private static String CLASS_NAME = AkkaHttpPrepareRequestsInstrumentation.class.getName();

    private static List<MethodMatcher<OpType>> methodMatchers = Arrays.asList(
            new MethodMatcher<OpType>("onPush", new String[] {}, "void", OpType.ON_PUSH)
    );

    private enum OpType {
        ON_PUSH
    }

    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes) throws Exception {
        for (final Entry<CtMethod, OpType> methodEntry : findMatchingMethods(cc, methodMatchers).entrySet()) {
            methodEntry.getKey().instrument(new ExprEditor() {
                public void edit(MethodCall m) throws CannotCompileException {
                    if (m.getMethodName().equals("push")) {
                        insertBeforeMethodCall(m, CLASS_NAME + ".handlePush($2);", false);
                    }
                }
            });
        }

        return true;
    }

    public static void handlePush(Object requestObject) {
        try {
            if (requestObject instanceof AkkaHttpRequest) {
                AkkaHttpRequest request = (AkkaHttpRequest) requestObject;
                String spanKey = request.tvGetHeader(ServletInstrumentation.X_SPAN_KEY);
                if (spanKey != null) {
                    Span span = SpanDictionary.getSpan(Long.valueOf(spanKey));
                    if (span != null) {
                        String remoteAddress = request.tvGetHeader("Remote-Address");
                        if (remoteAddress != null) {
                            span.setTag("ClientIP", remoteAddress);
                        }
                    }
                }
            }
        } catch (Throwable e) { //have to catch it here due to https://issues.jboss.org/browse/JASSIST-210
            logger.warn("Caught exception as below. Please take note that existing code flow should not be affected, this might only impact the instrumentation of current trace");
            logger.warn(e.getMessage(), e);
        }
    }
}