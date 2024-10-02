package com.tracelytics.instrumentation.http.ws;

import com.tracelytics.ext.javassist.*;
import com.tracelytics.instrumentation.FunctionClassHelper;
import com.tracelytics.instrumentation.MethodMatcher;
import com.tracelytics.instrumentation.Module;
import com.tracelytics.instrumentation.config.HideParamsConfig;
import com.tracelytics.instrumentation.http.ServletInstrumentation;
import com.tracelytics.instrumentation.http.play.PlayBaseInstrumentation;
import com.tracelytics.instrumentation.scala.ScalaUtil;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.Event;
import com.tracelytics.joboe.Metadata;
import com.tracelytics.joboe.OboeException;
import com.tracelytics.joboe.config.ConfigManager;
import com.tracelytics.joboe.config.ConfigProperty;
import com.tracelytics.util.HttpUtils;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Instrumentation for Play WS (Scala)
 * 
 * This instrumentation covers Play WS (Scala) version 2.1, 2.2, 2.3, 2.4 and 2.5.
 * 
 * Instrumentation for each version contains certain differences because the framework has different class hierarchy, methods and logic flow from version to version.
 * 
 * Best effort has been made to minimize the differences and share most of the code logic.
 * 
 * Basically, there are 4 tasks we need to accomplish for WS instrumentation:
 * <ol>
 *  <li>Start an extent by capturing the entry point with Http Method, target host and URL</li>
 *  <li>Set the x-trace header in the outbound Http request</li>
 *  <li>End an extent by capturing the exit point with Http status. As Play WS is designed to be asynchronous, the exit point would be on the Promise/Future redemption</li>
 *  <li>Parse the x-trace header in the inbound Http response and ensure the extent exit add an edge to it</li>
 * </ol>
 *  
 * Instrumentation details as below:
 *  
 * 2.4/2.5 Scala - Instruments class <code>play.api.libs.ws.WSRequest</code>. The <code>execute</code>, <code>stream</code> and <code>streamWithEnumerator</code> methods act as 
 * the entry point of the ws call. Unfortunately x-trace id cannot be set to request header directly (immutable) on this current instance like 2.3-. 
 * Therefore, in order to modify the request header before sending out the request, "tvWithXTraceHeader" is called with creates another immutable WSRequest instance.
 * The call is then forwarded to the new WSRequest instance which has the correct x-trace id in request header. In order to avoid infinite recursive call, we check
 * whether "generatedMetadata" is set, which indicates whether the header-injection and code forwarding has been done on this object. If it has, then the code should just proceed
 * with original code logic
 * 
 * The exit point is captured by adding callback to <code>transform</code> method in the returned <code>Future</code> instance
 * 
 * 2.3 Scala - Instruments class <code>play.api.libs.ws.WSRequest</code>, although it shares the same name as 2.4's class, the implementation is significantly different. In fact, 2.3 has another class
 * <code>play.api.lib.ws.WSRequestHolder</code> whose code is similar to 2.4's <code>play.api.libs.ws.WSRequest</code>. It might be feasible to reuse 2.4 instrumentation on 2.3's <code>play.api.lib.ws.WSRequestHolder</code>.
 * However we decided to instrument <code>play.api.libs.ws.WSRequest</code> directly instead, as one can makes calls without <code>WSRequestHolder</code> while <code>WSRequest</code> is mandatory.
 * Besides, <code>play.api.libs.ws.WSRequest</code> of 2.3 shares many similarity with 2.2's <code>play.api.libs.ws.WS$WSRequest</code>, therefore alot of code duplication is avoided. In the 2.3 version,
 * <code>setHeader</code> method is available so is the Http method getter <code>method()</code>. Therefore we do not have all those problems seen in version 2.4. We can simply start instrumentation
 * at <code>execute</code> method, create the entry event and then add the x-trace header in the <code>WSRequest</code> instance via <code>setHeader</code> method.
 * 
 * 2.2 and 2.1 Scala - Instruments class <code>play.api.libs.ws.WS$WSRequest</code>. Otherwise instrumentation is similar to 2.3 scala
 * 
 * 2.0 Scala - Not supported, it returns <code>play.api.libs.concurrent.Promise<code> instead of <code>scala.concurrent.Future</code> so all of the mapper has to be rewritten if we want support for it.
 * 
 * @author pluk
 *
 */
public class PlayScalaWsRequestInstrumentation extends PlayBaseInstrumentation {
    //Flag for whether hide query parameters as a part of the URL or not. By default false 
    protected static boolean hideUrlQuery = ConfigManager.getConfig(ConfigProperty.AGENT_HIDE_PARAMS) != null ? ((HideParamsConfig) ConfigManager.getConfig(ConfigProperty.AGENT_HIDE_PARAMS)).shouldHideParams(Module.WEB_SERVICE) : false;

    private static String CLASS_NAME = PlayScalaWsRequestInstrumentation.class.getName();
    
    private static Constructor<?> successFunctionConstructor;
    private static Constructor<?> failureFunctionConstructor;
    
    private static String LAYER_NAME = "play-ws";
    
    private enum OpType {
        EXECUTE, EXECUTE_STREAM 
    }
    
    private static ThreadLocal<Metadata> forkedContextThreadLocal = new ThreadLocal<Metadata>();
    
    
 // List of method matchers that declare method with signature patterns that we want to instrument
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<OpType>> methodMatchers = Arrays.asList(
            new MethodMatcher<OpType>("execute", new String[] { }, "scala.concurrent.Future", OpType.EXECUTE, true),
            new MethodMatcher<OpType>("streamWithEnumerator", new String[] { }, "scala.concurrent.Future", OpType.EXECUTE_STREAM, true), //2.5
            new MethodMatcher<OpType>("stream", new String[] { }, "scala.concurrent.Future", OpType.EXECUTE_STREAM, true), //2.4
            new MethodMatcher<OpType>("executeStream", new String[] { }, "scala.concurrent.Future", OpType.EXECUTE_STREAM, true), //2.3
            new MethodMatcher<OpType>("executeStream", new String[] { "scala.Function1" }, "scala.concurrent.Future", OpType.EXECUTE_STREAM, true) //2.2
    );
    
    
    

    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {
        Version version = identifyWsVersion();
        if (version == null) {
            logger.warn("Failed to recognize current Play scala WS version!");
            return false;
        }
        
        
        Map<CtMethod, OpType> matchingMethods = findMatchingMethods(cc, methodMatchers);
        
        //add success and failure function to catch the exit point
        synchronized(PlayScalaWsRequestInstrumentation.class) {
            if (successFunctionConstructor == null) {
                Class<?> mapperClass = createSuccessFunctionClass();
                if (mapperClass != null && mapperClass.getDeclaredConstructors().length > 0) {
                    successFunctionConstructor = mapperClass.getDeclaredConstructors()[0];
                }
            }
            if (failureFunctionConstructor == null) {
                Class<?> mapperClass = createFailureFunctionClass();
                if (mapperClass != null && mapperClass.getDeclaredConstructors().length > 0) {
                    failureFunctionConstructor = mapperClass.getDeclaredConstructors()[0];
                }
            }
        }
        
        //add fields/setter/getter for 2.4
        if (version.isNewerOrEqual(Version.PLAY_2_4)) {
            cc.addField(CtField.make("private String tvGeneratedMetadata;", cc));
            cc.addMethod(CtNewMethod.make("public void setTvGeneratedMetadata(String generatedMetadata) { tvGeneratedMetadata = generatedMetadata; }", cc));
            cc.addMethod(CtNewMethod.make("public String getTvGeneratedMetadata() { return tvGeneratedMetadata; }", cc));
            
            
            //add method to create request with x-trace header
            cc.addMethod(CtNewMethod.make("public " + PlayScalaWs2_4Request.class.getName() + " tvWithXTraceHeader(String metadata) { "
                                        + "    scala.collection.immutable.Seq xTraceHeader = scala.collection.immutable.$colon$colon$.MODULE$.apply(new scala.Tuple2(\"" + ServletInstrumentation.XTRACE_HEADER + "\", metadata), scala.collection.immutable.Nil$.MODULE$);"
                                        + "    return (" + PlayScalaWs2_4Request.class.getName() + ")withHeaders(xTraceHeader); "
                                        + "}", cc));
            
            tagInterface(cc, PlayScalaWs2_4Request.class.getName());
        } else if (version.isOlderOrEqual(Version.PLAY_2_3)) {
            cc.addMethod(CtNewMethod.make("public Object tvSetHeader(String headerKey, String headerValue) { "
                    + "    return setHeader(headerKey, headerValue); "
                    + "}", cc));
            tagInterface(cc, PlayScalaWs2_3MinusRequest.class.getName());
        }
        
        
        if (matchingMethods.containsValue(OpType.EXECUTE)) {
            ScalaUtil.addScalaContextForInstrumentation(cc); //then we need the context for our mapper execution
        }
        
        if (version.isNewerOrEqual(Version.PLAY_2_4)) {
            String patchedRequestClass = PlayScalaWs2_4Request.class.getName();
            for (Entry<CtMethod, OpType> methodEntry : matchingMethods.entrySet()) {
                CtMethod method = methodEntry.getKey();
                OpType type = methodEntry.getValue();
                
                if (type == OpType.EXECUTE || type == OpType.EXECUTE_STREAM) {
                    insertBefore(method, "if (this instanceof " + patchedRequestClass + ") {"
                                       +      patchedRequestClass + " wsRequest = (" + patchedRequestClass + ") this;"
                                       + "    play.api.libs.ws.WSRequest modifiedRequest = (play.api.libs.ws.WSRequest) " + CLASS_NAME + ".addXTraceHeader(wsRequest);"
                                       + "    if (modifiedRequest != null) {" //if request instance is modified, then we should call the same method on the modified instance instead
                                       +          CLASS_NAME + ".layerEntry2_4(( " + patchedRequestClass +" )modifiedRequest, " + (type == OpType.EXECUTE_STREAM) + ");"
                                       + "        return modifiedRequest. " + method.getName() + "($$);" 
                                       + "    }"
                                       + "}", false);
                                         
                    
                    
                    //wrap the Future by calling transform, include callback function so we can capture exit points
                    insertAfter(method, 
                          Metadata.class.getName() + " forkedContext = " + CLASS_NAME + ".consumeForkedContext();" +
                          "if (forkedContext != null && $_ instanceof scala.concurrent.Future) { "
                          + "Object successFunctionInstance = " + CLASS_NAME + ".getSuccessFunctionInstance(forkedContext);"
                          + "Object failureFunctionInstance = " + CLASS_NAME + ".getFailureFunctionInstance(forkedContext);"
                          + "if (successFunctionInstance instanceof scala.Function1 && failureFunctionInstance instanceof scala.Function1) {"
                          + "    $_  = $_.transform((scala.Function1)successFunctionInstance, (scala.Function1)failureFunctionInstance, " + ScalaUtil.EXECUTION_CONTEXT_FIELD + "); "
                          + "}"
                        + "}", true);
                } 
            }
        } else if (version.isOlderOrEqual(Version.PLAY_2_3)) {
            for (Entry<CtMethod, OpType> methodEntry : matchingMethods.entrySet()) {
                CtMethod method = methodEntry.getKey();
                OpType type = methodEntry.getValue();
                if (type == OpType.EXECUTE || type == OpType.EXECUTE_STREAM) {
                    insertBefore(method, CLASS_NAME + ".layerEntry2_3Minus(( " + PlayScalaWs2_3MinusRequest.class.getName() +" )this, " + (type == OpType.EXECUTE_STREAM) + ");", false);
                    //wrap the Future by calling transform, include callback function so we can capture exit points
                    insertAfter(method, 
                          Metadata.class.getName() + " forkedContext = " + CLASS_NAME + ".consumeForkedContext();" +
                          "if (forkedContext != null && $_ instanceof scala.concurrent.Future) { "
                          + "Object successFunctionInstance = " + CLASS_NAME + ".getSuccessFunctionInstance(forkedContext);"
                          + "Object failureFunctionInstance = " + CLASS_NAME + ".getFailureFunctionInstance(forkedContext);"
                          + "if (successFunctionInstance instanceof scala.Function1 && failureFunctionInstance instanceof scala.Function1) {"
                          + "    $_  = $_.transform((scala.Function1)successFunctionInstance, (scala.Function1)failureFunctionInstance, " + ScalaUtil.EXECUTION_CONTEXT_FIELD + "); "
                          + "}"
                        + "}", true);
                }
            }
        } 
        

        return true;
    }
    
    /**
     * Identifies the Play WS version, as instrumentation varies from version to version
     * @return
     */
    private Version identifyWsVersion() {
        try {
            CtClass interfaceClass = classPool.get("play.api.libs.ws.WSRequest"); //2.3 and 2.4 use class name "play.api.libs.ws.WSRequest"
            try {
                interfaceClass.getDeclaredMethod("withHeaders"); //2.4 has withHeaders method
                return Version.PLAY_2_4;
            } catch (NotFoundException e) { 
                try {
                    interfaceClass.getDeclaredMethod("setHeader"); //2.3 has setHeader method
                    return Version.PLAY_2_3;
                } catch (NotFoundException e1) {
                    return null;
                }
            }
        } catch (NotFoundException e) {
            try {
                classPool.get("play.api.libs.ws.WS$WSRequest"); //different name used in 2.2
                return Version.PLAY_2_2;
            } catch (NotFoundException e1) {
                return null;
            } 
        }
    }

    /**
     * Captures the http method used and create a new instance of WSRequset with the pre-generated x-trace header
     * @param request
     * @return a modified request object if x-trace is was injected during the call, null otherwise (null indicates the x-trace id was injected already outside of this current call)
     */
    public static Object addXTraceHeader(PlayScalaWs2_4Request request) {
        if (Context.getMetadata().isValid() && request.getTvGeneratedMetadata() == null) { //do not re-process otherwise
            String generatedMetadata;
            if (Context.getMetadata().isSampled()) {
                //generate a new task id ahead of time (since method "execute" is invoked after this), but we need to tag the x-trace id before the "execute"
                Metadata generatedContext = new Metadata(Context.getMetadata());
                generatedContext.randomizeOpID();
                generatedMetadata = generatedContext.toHexString();
            } else { //still propagate the non-tracing x-trace ID
                generatedMetadata = Context.getMetadata().toHexString();
            }
            
          //create the new object with generated x-trace header first, 
            PlayScalaWs2_4Request requestWithHeader = request.tvWithXTraceHeader(generatedMetadata);
            requestWithHeader.setTvGeneratedMetadata(generatedMetadata);
            return requestWithHeader;
        } else {
            return null; //null indicates no further modification is made to this request object 
        }
        
    }
    
    /**
     * Creates new instance of our Success function that calls our instrumentation layer exit
     * @param forkedContext
     * @return
     */
    public static Object getSuccessFunctionInstance(Metadata forkedContext) {
        if (successFunctionConstructor != null) {
            try {
                return successFunctionConstructor.newInstance(forkedContext);
            } catch (Exception e) {
                logger.warn("Failed to create a success function instance to track play layer exit");
            } 
        }
        return null;
    }
    
    /**
     * Creates new instance of our Failure function that calls our instrumentation layer exit
     * @param forkedContext
     * @return
     */
    public static Object getFailureFunctionInstance(Metadata forkedContext) {
        if (failureFunctionConstructor != null) {
            try {
                return failureFunctionConstructor.newInstance(forkedContext);
            } catch (Exception e) {
                logger.warn("Failed to create a failure function instance to track play layer exit");
            } 
        }
        return null;
    }
    
    /**
     * Gets and removes the forked context created by the layer entry event
     * @return
     */
    public static Metadata consumeForkedContext() {
        Metadata forkedContext = forkedContextThreadLocal.get();
        forkedContextThreadLocal.remove();
        return forkedContext;
    }
    
    private Class<?> createSuccessFunctionClass() throws CannotCompileException, NotFoundException, ClassNotFoundException {
        String callbackClassSimpleName = getClass().getSimpleName() + "SuccessFunction";
        FunctionClassHelper helper = FunctionClassHelper.getInstance(classPool, "scala.runtime.AbstractFunction1", callbackClassSimpleName);

        CtClass callbackClass = helper.getFunctionCtClass();
        callbackClass.addField(CtField.make("private " + Metadata.class.getName() + " context;", callbackClass));
        callbackClass.addMethod(CtNewMethod.make("public Object apply(Object obj) { "
                + "Object responseObject;"
                + "if (obj instanceof scala.Tuple2) {"
                + "    responseObject = ((scala.Tuple2)obj)._1; " //executeStream response
                + "} else {"
                + "    responseObject = obj; " 
                + "}"
                + CLASS_NAME + ".layerExit(context, responseObject);"
                + "context = null;"
           + "    return obj; "
           + "}", callbackClass));
        callbackClass.addConstructor(CtNewConstructor.make("public " + callbackClassSimpleName + "(" + Metadata.class.getName() + " forkedContext) { "
                + "context = forkedContext; "
                + "}", callbackClass));
        return helper.toFunctionClass();
    }
    
    private Class<?> createFailureFunctionClass() throws CannotCompileException, NotFoundException, ClassNotFoundException {
        String callbackClassSimpleName = getClass().getSimpleName() + "FailureFunction";
        FunctionClassHelper helper = FunctionClassHelper.getInstance(classPool, "scala.runtime.AbstractFunction1", callbackClassSimpleName);

        CtClass callbackClass = helper.getFunctionCtClass();
        callbackClass.addField(CtField.make("private " + Metadata.class.getName() + " context;", callbackClass));
        callbackClass.addMethod(CtNewMethod.make("public Object apply(Object throwable) { "
                + CLASS_NAME + ".layerExit(context, throwable);"
                + "context = null;"
           + "    return throwable; "
           + "}", callbackClass));
        callbackClass.addConstructor(CtNewConstructor.make("public " + callbackClassSimpleName + "(" + Metadata.class.getName() + " forkedContext) { "
                + "context = forkedContext; "
                + "}", callbackClass));
        return helper.toFunctionClass();
    }
    
    
    
    public static void layerEntry2_4(PlayScalaWs2_4Request request, boolean isStream) {
        Metadata existingContext = Context.getMetadata();
        
        if (existingContext.isSampled()) { //only handle if it's traced. For non-tracing x-trace id, it's already handled in addXTraceHeader
          //make a fork as it's asynchronous
            Metadata forkedContext = new Metadata(existingContext);
            
            Context.setMetadata(forkedContext);
            Event event;
            if (request.getTvGeneratedMetadata() != null) {
                try {
                    event = Context.createEventWithID(request.getTvGeneratedMetadata());
                } catch (OboeException e) {
                    logger.warn("Failed to use pre-generated x-trace id, the x-trace id will not be included in the x-trace header : " + request.getTvGeneratedMetadata());
                    event = Context.createEvent();
                }
            } else {
                logger.warn("Cannot find pre-generated x-trace id, the x-trace id will not be included in the x-trace header");
                event = Context.createEvent();
            }
            
            event.addInfo("Layer", LAYER_NAME,
                               "Label", "entry",
                               "IsService", true,
                               "Spec", "rsc",
                               "IsStream", isStream);
            
            if (request.method() != null) {
                event.addInfo("HTTPMethod", request.method());
            }
            if (request.url() != null) {
                event.addInfo("RemoteURL", hideUrlQuery ? HttpUtils.trimQueryParameters(request.url()) : request.url());
            }
            
            
            event.report();
            
            //set the context in the threadlocal such that on method exit, we can tag the context to the mapper functions
            forkedContextThreadLocal.set(forkedContext);
            
            //no need to set the header here as it has been added in addXTraceHeader
            
            Context.setMetadata(existingContext);
        }
    }
    
    public static void layerEntry2_3Minus(PlayScalaWs2_3MinusRequest request, boolean isStream) {
        Metadata existingContext = Context.getMetadata();
        
        if (existingContext.isSampled()) {
            //make a fork as it's asynchronous
            Metadata forkedContext = new Metadata(existingContext);
            
            Context.setMetadata(forkedContext);
            Event event = Context.createEvent();      
            event.addInfo("Layer", LAYER_NAME,
                               "Label", "entry",
                               "IsService", true,
                               "Spec", "rsc",
                               "IsStream", isStream);
            
            if (request.method() != null) {
                event.addInfo("HTTPMethod", request.method());
            }
            if (request.url() != null) {
                event.addInfo("RemoteURL", hideUrlQuery ? HttpUtils.trimQueryParameters(request.url()) : request.url());
            }
            
            event.report();
            
            //set the context in the threadlocal such that on method exit, we can tag the context to the mapper functions
            forkedContextThreadLocal.set(forkedContext);
            request.tvSetHeader(ServletInstrumentation.XTRACE_HEADER, forkedContext.toHexString());
            
            Context.setMetadata(existingContext);
        } else if (existingContext.isValid()) { //propagate the non-tracing x-trace ID
            request.tvSetHeader(ServletInstrumentation.XTRACE_HEADER, Context.getMetadata().toHexString());
        }
    }
    
    public static void layerExit(Metadata context, Object responseObject) {
        if (context != null && context.isSampled()) {
            Context.setMetadata(context);
            
            Event event = Context.createEvent();
            
            event.addInfo("Layer", LAYER_NAME,
                          "Label", "exit");
            event.setAsync();
            
            if (responseObject instanceof Throwable) {
                reportError(LAYER_NAME, (Throwable)responseObject);
            } else if (responseObject instanceof PlayScalaWsResponse) {
                PlayScalaWsResponse response = (PlayScalaWsResponse) responseObject;
                String responseXTraceId = response.tvGetXTraceHeaderValue();
                if (responseXTraceId != null) {
                    event.addEdge(responseXTraceId);
                }
                event.addInfo("HTTPStatus", response.status());
                if (response.statusText() != null && !"".equals(response.statusText())) {
                    event.addInfo("HTTPStatusText", response.statusText());
                }
            } else if (responseObject != null && responseObject.getClass().getName().startsWith("play.api.libs.iteratee.")) {
              //Skip for 2.2 and 2.1 for now
              //It is complicated to parse the x-trace response header and status code, by injecting in the executeStream(Iteratee) that 
              //  val newIteratee = iteratee.compose { headers : ResponseHeaders => 
              //    reportHeadersAndStatus(headers)   
              //    headers
              //  }
              //  iteratee = newIteratee
              //we might be able to capture the header and status code. The problem is however, to relate that to the Response, which I do not
              //see any trivial solutions (and they run on different threads)
              //One possible solution is to rely on the private WSRequest field $outer on the anonymous AsyncHandler declared within the WSRequest.executeStream
              //and tag the header info to that field when recieveHeaders/receiveStatusCode are invoked. Also when WSRequest.executeStream is called, we need to tag 
              //the same WSRequest instance to the Future object returned.(by adding extra mapping)
              //Now when the Future is redeemed it would have the WSRequest reference which has the header value set when recieveHeaders/receiveStatusCode are executed
              //This is very complicated and would make the instrumentation code unreadable. This is probably too big of a cost to do all that in order to get the status code and response header
              //for 2.2 and 2.1 streaming response
                logger.debug("2.1 or 2.2 stream response header, do not parse");
            } else {
                logger.warn("Unexpected response object for Play WS exit : " + responseObject);
            }
            
            event.report();
        } else {
            logger.warn("Failed to create Play WS exit, invalid context tagged to the mapper class " + (context != null ? context.toHexString() : "null"));
        }
    }
}