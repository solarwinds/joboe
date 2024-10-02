package com.tracelytics.instrumentation.http.spray;

import com.tracelytics.ext.javassist.*;
import com.tracelytics.instrumentation.FunctionClassHelper;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.MethodMatcher;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.span.impl.Span;
import com.tracelytics.joboe.span.impl.Span.TraceProperty;
import com.tracelytics.joboe.span.impl.SpanDictionary;
import com.tracelytics.joboe.span.impl.TraceEventSpanReporter;
import com.tracelytics.joboe.span.impl.Tracer;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

/**
 * Instruments http request and response handled by Spray routing. The extent entry is created when runSealRoute is called on <code>Spray.routing.HttpServiceBase$class</code>.
 * 
 * Take note that this might get called multiple times on the same request (for example if there is a timeout triggered within Spray Routing), therefore we will rely on 
 * the shared <code>HttpRequest</code> instance as a reference for processing on a certain request to avoid double instrumentation
 * 
 * The extent exit is captured by adding a customer "Mapper" (a Scala function that maps HttpResponse to HttpResponse) that calls our instrumentation logic when it's invoked.
 * This guarantees that an exit event will be created before the response is sent back. Take note that the mapper can also get called multiple times in some edge cases, therefore
 * we use the same tagged <code>HttpRequest</code> instance to prevent duplications
 * 
 * 
 * 
 * @author pluk
 *
 */
public class SprayHttpServiceInstrumentation extends ClassInstrumentation {

    private static String CLASS_NAME = SprayHttpServiceInstrumentation.class.getName();
    private static String LAYER_NAME = "spray-routing";

 // List of method matchers that declare method with signature patterns that we want to instrument
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<OpType>> methodMatchers = Arrays.asList(
         new MethodMatcher<OpType>("runSealedRoute$1", new String[] { "java.lang.Object", "spray.routing.RequestContext" }, "void", OpType.RUN_ROUTE)
    );
    
    private enum OpType {
        RUN_ROUTE
    }
    
    private static Constructor<?> mapperConstructor = null;

    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {
        loadMapperClass();
        for (Entry<CtMethod, OpType> methodEntry : findMatchingMethods(cc, methodMatchers).entrySet()) {
            //Start the extent on runSealedRoute$1
            insertBefore(methodEntry.getKey(),
                           "if ($2 != null) {"
                             + "spray.http.HttpRequest request = $2.request();"
                             + "String host = null;"
                             + "String remoteHost = null;"
                             + "String spanKey = null;"
                             //get the context from http request header, as the Spray can entry point is not on the same thread of Spray routing handling
                             + "if (request != null && request.headers() != null) {"
                             + "    for (int i = 0; i < request.headers().length(); i++) {"
                             + "        spray.http.HttpHeader header = (spray.http.HttpHeader)request.headers().apply(i);"
                             + "        if (header != null && \"" + X_SPAN_KEY + "\".equals(header.name())) {"
                             + "            spanKey = header.value();"
                             + "        }"
                             + "    }"
                             + "} "
                             +  Span.class.getName() + " span = " + CLASS_NAME + ".sprayRouteEntry(request, "
                                                                                                       + "spanKey, "
                                                                                                       + "$1, "
                                                                                                       + "request.method() != null ? request.method().name() : null, "
                                                                                                       + "request.uri().path() != null ? request.uri().path().toString() : null);"
                             //add mapping to capture extent exit point
                             + "if (span != null) {"
                             + "    Object mapperInstance = " + CLASS_NAME + ".getMapperInstance(request);"
                             + "    if (mapperInstance != null) {"
                             + "        $2 = $2.withHttpResponseMapped((scala.Function1)mapperInstance);" 
                             + "    }" 
                             + "}"
                          + "}"
                         , false);
            insertAfter(methodEntry.getKey(), Context.class.getName() +  ".clearMetadata();", true, false); //always clear context - traced or not
        }
        return true;
    }
    
    public static Object getMapperInstance(Object requestObject) {
        if (mapperConstructor != null) {
            try {
                return mapperConstructor.newInstance(requestObject);
            } catch (Exception e) {
                logger.warn("Failed to construct mapper instance for Spray routing exit instrumentation.");
            } 
        } 

        logger.warn("Failed to load the Mapper class for Spray routing exit instrumentation.");
        return null;
    }
    
    /**
     * Get our own mapper class, that basically does no mapping (just return the same instance), but call our instrumentation logic for exit events.
     * 
     * Take note that we only want to generate the class on first call, all subsequent calls should just return the mapper class name w/o creating the class again
     *  
     * @return
     * @throws CannotCompileException
     * @throws NotFoundException
     */
    private String loadMapperClass() throws CannotCompileException, NotFoundException, ClassNotFoundException {
        final String CALLBACK_CLASS_NAME = "TvHttpResponseMapper";
        CtClass callbackClass;
        
        synchronized(SprayHttpServiceInstrumentation.class) { //lock it to avoid duplicated instantiation on concurrent call
            try {
                callbackClass = classPool.get(CALLBACK_CLASS_NAME);
            } catch (NotFoundException e) {
                String callbackClassSimpleName = SprayHttpServiceInstrumentation.class.getSimpleName() + "FinishFunction";
                FunctionClassHelper helper = FunctionClassHelper.getInstance(classPool, "scala.runtime.AbstractFunction1", callbackClassSimpleName);

                callbackClass = helper.getFunctionCtClass();

                callbackClass.addField(CtField.make("private Object request;", callbackClass));
                callbackClass.addMethod(CtNewMethod.make(
                 "public java.lang.Object apply(java.lang.Object obj) { "
               +      CLASS_NAME + ".sprayRouteExit((obj instanceof spray.http.HttpResponse) && ((spray.http.HttpResponse)obj).status() != null ? ((spray.http.HttpResponse)obj).status().intValue() : -1, request);"
               + "    return obj; "
               + "}", callbackClass));
                
                callbackClass.addConstructor(CtNewConstructor.make("public " + callbackClassSimpleName + "(Object request) { "
                        + "this.request = request;"
                        + "}", callbackClass));
                
                Class<?> mapperClass = helper.toFunctionClass();
                //have to keep a reference to the constructor, so we can invoke this one
                //as code that invokes the class instantiation might not be the same class loader of the current one (hence might throw ClassNotFoundException/NoClassDefFoundError)
                mapperConstructor = mapperClass.getConstructors()[0];
            }
        }
        
        return callbackClass.getName();
    }
    
   
    public static Span sprayRouteEntry(Object httpRequestObject, String parentSpanKey, Object actorObject, String httpMethod, String path) {
        if (!(httpRequestObject instanceof SprayHttpRequest)) {
            logger.warn(httpRequestObject + " is not properly tagged as " + SprayHttpRequest.class.getName());
            return null;
        }
        
        SprayHttpRequest httpRequest = (SprayHttpRequest) httpRequestObject;
        
        if (parentSpanKey == null) {
            logger.debug("Cannot create entry event for spray routing as parent span key is null, probably not started with supported entry layer"); 
            return null;
        }
        
        Span parentSpan = SpanDictionary.getSpan(Long.valueOf(parentSpanKey));
        
        if (parentSpan == null) { //take note that this could happen for edge case when the spray-can time outs and the spray route issues another handling
        	logger.debug("Cannot create entry event for spray routing as span is not found with key " + parentSpanKey + " the parent span has exited"); 
            return null;
        }
        
        String action = null;
        String controller = null;
        if (httpMethod != null && path != null) {
            action = httpMethod.toLowerCase() + "(" + path + ")";
        }
        
        if (actorObject != null) {
            controller = actorObject.getClass().getName();
        }
        
        Span routingSpan = httpRequest.getTvSprayRoutingSpan();
      //only start extent if a trace the span is sampled
        if (parentSpan.context().isSampled()) {  
            //only create entry event if an extent has not been started yet. This is to avoid double tracing (such as from timeout). 
            //Take note that this might not work if the httpRequest object is cloned in the flow (not the case now)
        	
        	 if (routingSpan == null) {
            	routingSpan = Tracer.INSTANCE.buildSpan(LAYER_NAME).withReporters(TraceEventSpanReporter.REPORTER).asChildOf(parentSpan.context()).start();
                
         	    if (action != null) {
           	        routingSpan.setTag("Action", action);
           	    }
                
                httpRequest.setTvSprayRoutingSpan(routingSpan);
            } 
            //update the actor name as we want to use the Last controller used
            if (controller != null) { 
                routingSpan.setTag("Controller", controller);
            }
            
        } else { //not sampled for tracing
            //still create a routing span in order to report Action/Controller (for metrics). But it's no-op
            routingSpan = Tracer.INSTANCE.buildSpan(LAYER_NAME).asChildOf(parentSpan.context()).start();
            httpRequest.setTvSprayRoutingSpan(routingSpan);
            
            //propagate the non-traced context
            Context.setMetadata(parentSpan.context().getMetadata());
        }
        
        
        if (action != null) {
            routingSpan.setTracePropertyValue(TraceProperty.ACTION, action);
        }
        if (controller != null) {
            routingSpan.setTracePropertyValue(TraceProperty.CONTROLLER, controller);
        }
        
        return routingSpan;
    }
    
    public static void sprayRouteExit(int statusCode, Object httpRequestObject) {
        if (!(httpRequestObject instanceof SprayHttpRequest)) {
            logger.warn(httpRequestObject + " is not properly tagged as " + SprayHttpRequest.class.getName());
            return;
        }
        
        
        Span span = ((SprayHttpRequest)httpRequestObject).getTvSprayRoutingSpan();
      //in some edge cases (such as timeout), this could be called on multiple times, we will only capture the first one as the active context would be removed from the http request object after the first exit
        if (span != null) {
        	span.setTag("Status", statusCode);
        	
            span.finish();
            
            //cleanup
            span.context().getMetadata().initialize(); //clean up the context started by sprayRouteEntry
            ((SprayHttpRequest)httpRequestObject).setTvSprayRoutingSpan(null);
        }
    }
}