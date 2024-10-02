package com.tracelytics.instrumentation.http.akka.server;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.ext.javassist.CtNewMethod;
import com.tracelytics.ext.javassist.NotFoundException;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.http.ServletInstrumentation;
import com.tracelytics.joboe.XTraceOptionsResponse;
import com.tracelytics.joboe.span.impl.Span;
import com.tracelytics.joboe.span.impl.SpanDictionary;

/**
 * Instruments the `GraphStageLogic` of the `ControllerStage` of `HttpServerBluePrint`
 *
 * 1. Patches this to expose the current active `RequestStart` being handled. This is useful for context/span propagation
 * from span start point to span exit point
 * 2. Instruments the method `emitErrorResponse` to capture low level exception and finishes the corresponding span
 *
 */
public class AkkaHttpControllerStageLogicInstrumentation extends ClassInstrumentation {
    private static String CLASS_NAME = AkkaHttpControllerStageLogicInstrumentation.class.getName();

    @Override
    protected boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes) throws Exception {
        try {
            //Pretty fragile to look up such a private field, however this is the best reference point for context propagation we got so far...
            cc.getDeclaredField("akka$http$impl$engine$server$HttpServerBluePrint$ControllerStage$$anon$$openRequests");

            cc.addMethod(CtNewMethod.make("public " + AkkaHttpRequestStart.class.getName() + " tvGetCurrentRequest() {" +
                    "    Object result = akka$http$impl$engine$server$HttpServerBluePrint$ControllerStage$$anon$$openRequests.isEmpty() ? null : akka$http$impl$engine$server$HttpServerBluePrint$ControllerStage$$anon$$openRequests.head();" +
                    "    if (result instanceof " + AkkaHttpRequestStart.class.getName() + ") { return (" + AkkaHttpRequestStart.class.getName() + ") result; } else { return null; }" +
                    "}", cc));
            tagInterface(cc, AkkaHttpCurrentRequestAware.class.getName());

            CtClass httpResponseType = classPool.get("akka.http.scaladsl.model.HttpResponse");
            for (CtMethod method : cc.getDeclaredMethods()) {
                if (method.getName().endsWith("emitErrorResponse")) { //all of the error handling within `ControllerStage` would eventually call this
                    if (method.getParameterTypes().length == 1 && method.getParameterTypes()[0].subtypeOf(httpResponseType)) {
                        insertBefore(method, "$1 = (akka.http.scaladsl.model.HttpResponse) " + CLASS_NAME + ".endTraceOnErrorResponse($1, this);", false);
                    }
                }
            }
            return true;
        } catch (NotFoundException e) {
            logger.warn("Cannot find the akka$http$impl$engine$server$HttpServerBluePrint$ControllerStage$$anon$$openRequests field. Akka http server instrumentation will not be able to finish the span properly");
            return false;
        }
    }

    /**
     * On top of the exception handling by higher level `akka.http.scaladsl.server.ExceptionHandler`, if any low level exception is thrown,
     * it will be captured here and the corresponding span will be finished
     * @param throwable
     * @param currentRequestAwareObject
     */

    public static Object endTraceOnErrorResponse(Object httpResponseObject, Object logicStageObject) {
        if (httpResponseObject instanceof AkkaHttpResponse && logicStageObject instanceof AkkaHttpCurrentRequestAware) {
            AkkaHttpResponse httpResponse = (AkkaHttpResponse) httpResponseObject;
            AkkaHttpRequestStart currentRequest = ((AkkaHttpCurrentRequestAware) logicStageObject).tvGetCurrentRequest();
            if (currentRequest != null) {
                String spanKey = currentRequest.tvGetHeader(ServletInstrumentation.X_SPAN_KEY);
                if (spanKey != null) {
                    Span span = SpanDictionary.getSpan(Long.parseLong(spanKey));
                    if (span != null) {
                        span.setTag("Status", ((AkkaHttpResponse) httpResponseObject).tvStatusCode());
                        span.finish();

                        SpanDictionary.removeSpan(span);

                        XTraceOptionsResponse xTraceOptionsResponse = XTraceOptionsResponse.computeResponse(span);
                        if (xTraceOptionsResponse != null) {
                            httpResponse = httpResponse.tvAddHeader(ServletInstrumentation.X_TRACE_OPTIONS_RESPONSE_KEY, xTraceOptionsResponse.toString());
                        }

                        return httpResponse.tvAddHeader(ServletInstrumentation.XTRACE_HEADER, span.getSpanPropertyValue(Span.SpanProperty.EXIT_XID));
                    }
                }
            }
        }

        logger.warn("Failed to end Akka http trace");
        return httpResponseObject; //return the original object without x-trace header
    }

}
