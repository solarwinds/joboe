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
import com.tracelytics.joboe.config.ConfigManager;
import com.tracelytics.joboe.config.ConfigProperty;
import com.tracelytics.util.HttpUtils;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Instrumentation for Play WS (Java)
 *
 * This instrumentation covers Play WS (Java) version 2.2+ (up to 2.7 as tested)
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
 * 2.6+ Java - The framework no longer always calls <code>execute</code> on `WSRequest`, instead it calls the "delegate" `StandaloneWSRequest`, which the <code>execute</code> method is invoked.
 * Theefore we will instrument `StandaloneWSRequest` instead of `WSRequest` for 2.6+
 *
 * 2.5 Java - Similar to 2.4, except that <code>execute</code> returns <code>java.util.function.Function</code> instance instead of <code>play.libs.F.Function</code>.
 *
 * 2.4 Java - Instruments class <code>play.libs.ws.WSRequest</code>. The <code>execute</code> method acts as the entry point of the ws call. Unfortunately the base execute method does not take
 * any parameter, nor any of the method of the object directly provides Http method used. Therefore in order to obtain the Http method used, extra patching is done on the
 * <code>setMethod</code> method, which is supposed to be invoked before the base <code>execute</code>, to capture the http method used,
 *
 * The exit point is captured by adding callback to <code>transform</code> method in the returned <code>play.libs.F$Promise</code> instance
 *
 * 2.3 Java - Instruments class <code>play.libs.ws.WSRequest</code>, it has the <code>getMethod</code>, therefore we do not need the patching on <code>setHeader</code> like in 2.4. Layer exit
 * capturing is also the same as 2.4
 *
 * 2.2 Java - Instruments class <code>play.libs.ws.WSRequest</code>, it has the <code>getMethod</code>, therefore we do not need the patching on <code>setHeader</code> like in 2.4. Layer exit
 * capturing is not the same as 2.3/2.4 as it does NOT have <code>transform</code> method in <code>play.libs.F$Promise</code>, instead it we would have to first call <code>map</code> and then
 * <code>recover</code> on the <code>play.libs.F$Promise</code> instance.
 *
 * 2.0 - 2.1 Java - Not supported, the different design of <code>play.libs.F$Promise</code> makes our exit point capturing impossible with the existing approach. Calling <code>map</code> on
 * the <code>Promise</code> in 2.1 does return a wrapper/mapped <code>Promise</code> unfortunately, this <code>Promise</code> does not get redeemed until the <code>Action</code> exits (instead
 * of the expected behavior in 2.2+, which the wrapper <code>Promise</code> is redeem when the wrapped <code>Promise</code> is). This different behavior has caused timeout on
 * instrumented <code>WSRequest</code>. It might be possible to get the wrapped scala <code>Future</code> within the <code>Promise</code> and wrap that instead, but it will be very
 * complicated.
 *
 *
 * @author pluk
 *
 */
public class PlayJavaWsRequestInstrumentation extends PlayBaseInstrumentation {
    //Flag for whether hide query parameters as a part of the URL or not. By default false
    protected static boolean hideUrlQuery = ConfigManager.getConfig(ConfigProperty.AGENT_HIDE_PARAMS) != null ? ((HideParamsConfig) ConfigManager.getConfig(ConfigProperty.AGENT_HIDE_PARAMS)).shouldHideParams(Module.WEB_SERVICE) : false;

    private static String CLASS_NAME = PlayJavaWsRequestInstrumentation.class.getName();

    private static Constructor<?> successFunctionConstructor;
    private static Constructor<?> failureFunctionConstructor;

    private static String LAYER_NAME = "play-ws";

    private enum OpType {
        EXECUTE, SET_METHOD, STREAM
    }

    private static ThreadLocal<Metadata> forkedContextThreadLocal = new ThreadLocal<Metadata>();


 // List of method matchers that declare method with signature patterns that we want to instrument
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<OpType>> methodMatchers = Arrays.asList(
            new MethodMatcher<OpType>("execute", new String[] { }, "play.libs.F$Promise", OpType.EXECUTE, true), //2.4-
            new MethodMatcher<OpType>("execute", new String[] { }, "java.util.concurrent.CompletionStage", OpType.EXECUTE, true), //2.5
            new MethodMatcher<OpType>("stream", new String[] { }, "java.util.concurrent.CompletionStage", OpType.EXECUTE, true), //2.5
            new MethodMatcher<OpType>("setMethod", new String[] { "java.lang.String" }, "play.libs.ws.WSRequest", OpType.SET_METHOD, true) //only for 2.4, as it no longer offers getMethod
    );




    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {
        Version version = identifyWsVersion();

        if (version == null) {
            logger.warn("Failed to recognize current Play java WS version!");
            return false;
        } else if (version.isOlderOrEqual(Version.PLAY_2_1)) {
            logger.debug("Not tracing Play java ws 2.1");
            return false;
        }

        if (version.isNewerOrEqual(Version.PLAY_2_6)) {
            CtClass standaloneWSRequestClass = classPool.get("play.libs.ws.StandaloneWSRequest"); //for 2.6, patches only play.libs.ws.StandaloneWSRequest
            if (!cc.subtypeOf(standaloneWSRequestClass)) {
                return false;
            }
        }

        patchInterface(cc, version);

        Map<CtMethod, OpType> matchingMethods = findMatchingMethods(cc, methodMatchers);

        synchronized(PlayJavaWsRequestInstrumentation.class) {
            if (successFunctionConstructor == null) {
                Class<?> mapperClass = createSuccessFunctionClass(version);
                if (mapperClass != null && mapperClass.getDeclaredConstructors().length > 0) {
                    successFunctionConstructor = mapperClass.getDeclaredConstructors()[0];
                }
            }
            if (failureFunctionConstructor == null) {
                Class<?> mapperClass = createFailureFunctionClass(version);
                if (mapperClass != null && mapperClass.getDeclaredConstructors().length > 0) {
                    failureFunctionConstructor = mapperClass.getDeclaredConstructors()[0];
                }
            }
        }

        String setHeaderMethodName = version.isNewerOrEqual(Version.PLAY_2_6) ? "addHeader" : "setHeader";
        cc.addMethod(CtNewMethod.make("public Object tvSetHeader(String headerKey, String headerValue) { "
                + "    return " + setHeaderMethodName + "(headerKey, headerValue); "
                + "}", cc));
        tagInterface(cc, PlayJavaWsRequest.class.getName());


       //Check and see if scala context is necessary
        if (matchingMethods.containsValue(OpType.EXECUTE)) { 
            ScalaUtil.addScalaContextForInstrumentation(cc); //then we need the context for our mapper execution
        }

       for (Entry<CtMethod, OpType> methodEntry : matchingMethods.entrySet()) {
            CtMethod method = methodEntry.getKey();
            OpType type = methodEntry.getValue();
            if (type == OpType.EXECUTE || type == OpType.STREAM) {
                insertBefore(method, CLASS_NAME + ".layerEntry(( " + PlayJavaWsRequest.class.getName() +" )this);", false);
                if (version.isNewerOrEqual(Version.PLAY_2_5))  {
                    insertAfter(method,
                            Metadata.class.getName() + " forkedContext = " + CLASS_NAME + ".consumeForkedContext();"
                          + "if ($_ != null) { "
                          + "    Object successFunctionInstance = " + CLASS_NAME + ".getSuccessFunctionInstance(forkedContext);"
                          + "    Object failureFunctionInstance = " + CLASS_NAME + ".getFailureFunctionInstance(forkedContext, true);"
                          + "    if (successFunctionInstance instanceof java.util.function.Function && failureFunctionInstance instanceof java.util.function.Function) {"
                          + "        $_  = $_.thenApply((java.util.function.Function)successFunctionInstance).exceptionally((java.util.function.Function)failureFunctionInstance);"
                          + "    }"
                          + "}"
                          , true);
                } else if (version.isNewerOrEqual(Version.PLAY_2_3)) {
                    insertAfter(method,
                              Metadata.class.getName() + " forkedContext = " + CLASS_NAME + ".consumeForkedContext();"
                            + "if ($_ != null) { "
                            + "    Object successFunctionInstance = " + CLASS_NAME + ".getSuccessFunctionInstance(forkedContext);"
                            + "    Object failureFunctionInstance = " + CLASS_NAME + ".getFailureFunctionInstance(forkedContext, false);"
                            + "    if (successFunctionInstance instanceof play.libs.F.Function && failureFunctionInstance instanceof play.libs.F.Function) {"
                            + "        $_  = $_.transform((play.libs.F.Function)successFunctionInstance, (play.libs.F.Function)failureFunctionInstance, " + ScalaUtil.EXECUTION_CONTEXT_FIELD + ");"
                            + "    }"
                            + "}"
                            , true);
                } else if (version == Version.PLAY_2_2) { //no transform method on the Promise object returned. Use map then recover
                    insertAfter(method,
                              Metadata.class.getName() + " forkedContext = " + CLASS_NAME + ".consumeForkedContext();"
                            + "if ($_ != null) { "
                            + "    Object successFunctionInstance = " + CLASS_NAME + ".getSuccessFunctionInstance(forkedContext);"
                            + "    Object failureFunctionInstance = " + CLASS_NAME + ".getFailureFunctionInstance(forkedContext, true);"
                            + "    if (successFunctionInstance instanceof play.libs.F.Function && failureFunctionInstance instanceof play.libs.F.Function) {"
                            //does not have transform, has to use map then recover, which in recover we should rethrow exception
                            + "        $_  = $_.map((play.libs.F.Function)successFunctionInstance, " + ScalaUtil.EXECUTION_CONTEXT_FIELD + ").recover((play.libs.F.Function)failureFunctionInstance, " + ScalaUtil.EXECUTION_CONTEXT_FIELD + ");"
                            + "    }"
                            + "}"
                            , true);
                }
            } else if (type == OpType.SET_METHOD) {
                try {
                    cc.getDeclaredField("tvHttpMethod");
                    insertBefore(method, "tvHttpMethod = $1;");
                } catch (NotFoundException e) {
                    //expected if tvHttpMethod field is not necessary
                }
            }
        }

        return true;
    }


    private void patchInterface(CtClass cc, Version version) throws CannotCompileException, NotFoundException {
        try {
           cc.getMethod("getMethod", "()Ljava/lang/String;");
        } catch (NotFoundException e) {
           cc.addField(CtField.make("protected String tvHttpMethod;", cc));
           cc.addMethod(CtNewMethod.make("public String getMethod() { return tvHttpMethod; }", cc));
        }

        tagInterface(cc, PlayJavaWsRequest.class.getName());
    }

    /**
     * Identifies the Play WS version, as instrumentation varies from version to version
     * @return
     */
    private Version identifyWsVersion() {
        try {
            classPool.get("play.libs.ws.StandaloneWSRequest");
            return Version.PLAY_2_6;
        } catch (NotFoundException e) {
            //ok not 2.6+
        }

        try {
            CtClass interfaceClass = classPool.get("play.libs.ws.WSRequest"); //2.3 + use class name "play.libs.ws.WSRequest"

            CtMethod executeMethod = interfaceClass.getDeclaredMethod("execute");

            if (executeMethod.getReturnType().getName().equals("java.util.concurrent.CompletionStage")) {
                return Version.PLAY_2_5;
            }

            try {
                interfaceClass.getDeclaredMethod("setMethod");
                return Version.PLAY_2_4;
            } catch (NotFoundException e) {
                try {
                    interfaceClass.getDeclaredMethod("getMethod");
                    return Version.PLAY_2_3;
                } catch (NotFoundException e1) {
                    return null;
                }
            }
        } catch (NotFoundException e) {
            try {
                CtClass promiseClass = classPool.get("play.libs.F$Promise");
                try {
                    promiseClass.getDeclaredMethod("wrapped"); //only 2.2 has wrapped method
                    return Version.PLAY_2_2;
                } catch (NotFoundException e1) {
                    return Version.PLAY_2_1;
                }
            } catch (NotFoundException e1) {
                return null;
            }
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
                logger.warn("Failed to create a success function instance to track Play java WS layer exit");
            }
        }
        return null;
    }

    /**
     * Creates new instance of our Failure function that calls our instrumentation layer exit
     * @param forkedContext
     * @param rethrow       whether to rethrow the exception or not. If this instance is passed into the <code>recover</code> handling, then we need to rethrow it. Otherwise in 
     * <code>transform<code> handling, it only needs to return the exception instead of rethrowing it
     * @return
     */
    public static Object getFailureFunctionInstance(Metadata forkedContext, boolean rethrow) {
        if (failureFunctionConstructor != null) {
            try {
                return failureFunctionConstructor.newInstance(forkedContext, rethrow);
            } catch (Exception e) {
                logger.warn("Failed to create a failure function instance to track Play java WS layer exit");
            }
        }
        return null;
    }

    /**
     * Creates new instance of our Success function that calls our instrumentation layer exit
     * @return
     */
    public static Metadata consumeForkedContext() {
        Metadata forkedContext = forkedContextThreadLocal.get();
        forkedContextThreadLocal.remove();
        return forkedContext;
    }

    private Class<?> createSuccessFunctionClass(Version version) throws CannotCompileException, NotFoundException, ClassNotFoundException {
        String callbackClassSimpleName = getClass().getSimpleName() + "SuccessFunction";
        FunctionClassHelper helper;
        if (version.isNewerOrEqual(Version.PLAY_2_5)) {
            helper = FunctionClassHelper.getInstance(classPool, CLASS_NAME, "java.util.function.Function", callbackClassSimpleName);
        } else {
            helper = FunctionClassHelper.getInstance(classPool, "play.libs.F$Function", callbackClassSimpleName);
        }
        CtClass callbackClass = helper.getFunctionCtClass();

        callbackClass.addField(CtField.make("private " + Metadata.class.getName() + " context;", callbackClass));
        callbackClass.addMethod(CtNewMethod.make("public Object apply(Object obj) { "
                + CLASS_NAME + ".layerExit(context, obj);"
                + "context = null;"
           + "    return obj; "
           + "}", callbackClass));
        callbackClass.addConstructor(CtNewConstructor.make("public " + callbackClassSimpleName + "(" + Metadata.class.getName() + " forkedContext) { "
                + "context = forkedContext; "
                + "}", callbackClass));
        return helper.toFunctionClass();
    }

    private Class<?> createFailureFunctionClass(Version version) throws CannotCompileException, NotFoundException, ClassNotFoundException {
        String callbackClassSimpleName = getClass().getSimpleName() + "FailureFunction";
        FunctionClassHelper helper;
        if (version.isNewerOrEqual(Version.PLAY_2_5)) {
            helper = FunctionClassHelper.getInstance(classPool, CLASS_NAME,"java.util.function.Function", callbackClassSimpleName);
        } else {
            helper = FunctionClassHelper.getInstance(classPool, "play.libs.F$Function", callbackClassSimpleName);
        }
        CtClass callbackClass = helper.getFunctionCtClass();

        callbackClass.addField(CtField.make("private " + Metadata.class.getName() + " context;", callbackClass));
        callbackClass.addField(CtField.make("private boolean rethrow;", callbackClass));
        callbackClass.addMethod(CtNewMethod.make(
                  "public Object apply(Object throwable) { "
                +      CLASS_NAME + ".layerExit(context, throwable);"
                + "    context = null;"
                + "    if (rethrow && throwable instanceof Throwable) {"
                + "        throw (Throwable)throwable;"
                + "    } else {  "
                + "        return throwable; "
                + "    }"
                + "}", callbackClass));
        callbackClass.addConstructor(CtNewConstructor.make("public " + callbackClassSimpleName + "(" + Metadata.class.getName() + " forkedContext, boolean rethrow) { "
                + "context = forkedContext; "
                + "this.rethrow = rethrow;"
                + "}", callbackClass));
        return helper.toFunctionClass();
    }

    public static void layerEntry(PlayJavaWsRequest request) {

        Metadata existingContext = Context.getMetadata();

        if (existingContext.isSampled()) {
          //make a fork as it's asynchronous
            Metadata forkedContext = new Metadata(existingContext);

            Context.setMetadata(forkedContext);
            Event event = Context.createEvent();
            event.addInfo("Layer", LAYER_NAME,
                           "Label", "entry",
                           "IsService", true,
                           "Spec", "rsc");

            if (request.getMethod() != null) {
                event.addInfo("HTTPMethod", request.getMethod());
            }
            if (request.getUrl() != null) {
                event.addInfo("RemoteURL", hideUrlQuery ? HttpUtils.trimQueryParameters(request.getUrl()) : request.getUrl());
            }

            event.report();

            //set the context in the threadlocal such that on method exit, we can tag the context to the mapper functions
            forkedContextThreadLocal.set(forkedContext);
            request.tvSetHeader(ServletInstrumentation.XTRACE_HEADER, forkedContext.toHexString());
            Context.setMetadata(existingContext);
        } else if (existingContext.isValid()) { //still propagate the non-tracing x-trace id 
            request.tvSetHeader(ServletInstrumentation.XTRACE_HEADER, existingContext.toHexString());
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
            } else if (responseObject instanceof PlayJavaWsResponse) {
                PlayJavaWsResponse response = (PlayJavaWsResponse) responseObject;
                String responseXTraceId = response.getHeader(ServletInstrumentation.XTRACE_HEADER);
                if (responseXTraceId != null) {
                    event.addEdge(responseXTraceId);
                }
                event.addInfo("HTTPStatus", response.getStatus());
                if (response.getStatusText() != null && !"".equals(response.getStatusText())) {
                    event.addInfo("HTTPStatusText", response.getStatusText());
                }
            } else {
                logger.warn("Unexpected response object for Play java WS exit : " + responseObject);
            }

            event.report();
        } else {
            logger.warn("Failed to create Play java WS exit, invalid context tagged to the mapper class " + context != null ? context.toHexString() : "null");
        }
    }


}