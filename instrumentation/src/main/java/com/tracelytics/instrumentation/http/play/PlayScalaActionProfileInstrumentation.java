package com.tracelytics.instrumentation.http.play;

import java.lang.reflect.Constructor;
import java.util.*;

import com.tracelytics.ext.javassist.CannotCompileException;
import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtField;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.ext.javassist.CtNewConstructor;
import com.tracelytics.ext.javassist.CtNewMethod;
import com.tracelytics.ext.javassist.NotFoundException;
import com.tracelytics.instrumentation.FunctionClassHelper;
import com.tracelytics.instrumentation.MethodMatcher;
import com.tracelytics.instrumentation.scala.ScalaUtil;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.Metadata;
import com.tracelytics.joboe.span.impl.*;
import com.tracelytics.joboe.span.impl.Span.TraceProperty;

/**
 * Instruments the Play 2 Action with Scala template.
 *
 * A typical Play action declaration:
 * <code>
 * def myAction = Action { request =>
 *   //some processing
 *   Ok(views.html.index("done!"))
 * }
 * </code>
 *
 * For instrumentation, the Play layer should capture the duration of invoking the block function: Request => Result
 *
 * However, Play also encourages asynchronous Result (actually all Play actions are asynchronous by default, even the synchronous example above is wrapped with Future[Result] object).
 *
 * For code below
 * <code>
 * def asyncAction = Action.async { request =>
 *   val computationFuture = expensiveComputation() //some longer processing that returns future
 *   computationFuture.map( computationResult => Ok(views.html.index("done!")))
 * }
 * </code>
 *
 * We cannot just instrument the entry and exit on the block function : Request => Future[Result], as the Future[Result] is probably returned before the computation actually completes,
 * The entry of the layer can still be the start of invocation of block function: Request => Future[Result], but it should exit at the mapping on Any => Result (the computation => Ok...) instead
 *
 *
 * Therefore the current implementation capture:
 * <ol>
 *  <li>Entry event: beginning of invokeBlock call, which calls the Request => Future[Result] logic defined in customer's application</li>
 *  <li>Exit event: when Future[Result] was redeemed. We added an 2 extra callbacks as .transform(successFunction, failureFunction) with the functions creating the exit event</li>
 * </ol>
 *
 * Technically we might be able to just create 1 instance of callback and shared for both the success and failure mapping. However, Throwable might be passed into the success
 * function in certain edge case (for example when result is Result[Throwable]), we will mistakenly recognize that as failure mapping
 *
 * @author pluk
 *
 */
public class PlayScalaActionProfileInstrumentation extends PlayBaseInstrumentation {

    private static String CLASS_NAME = PlayScalaActionProfileInstrumentation.class.getName();

    private static Constructor<?> successFunctionConstructor;
    private static Constructor<?> failureFunctionConstructor;
    private static ThreadLocal<Integer> functionDepthThreadLocal = new ThreadLocal<Integer>();

    // List of method matchers that declare method with signature patterns that we want to instrument
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<OpType>> methodMatchers = Arrays.asList(
            new MethodMatcher<OpType>("invokeBlock", new String[] { "play.api.mvc.Request", "scala.Function1" }, "scala.concurrent.Future", OpType.INVOKE_BLOCK)
    );


    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
            throws Exception {

        synchronized(PlayScalaActionProfileInstrumentation.class) {
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

        Set<CtMethod> matchingMethods = findMatchingMethods(cc, methodMatchers).keySet();
        if (!matchingMethods.isEmpty()) {
            ScalaUtil.addScalaContextForInstrumentation(cc); //then we need the context for our mapper execution

            Version version = identifyVersion();

            for (CtMethod method : matchingMethods) {
                if (version.isNewerOrEqual(Version.PLAY_2_6)) {
                    insertBefore(method,
                            "String controller = null;" +
                                    "String action = null;" +
                                    "if ($1.attrs().contains(play.api.routing.Router$Attrs$.MODULE$.HandlerDef())) {" +
                                    "    play.api.routing.HandlerDef handlerRef = (play.api.routing.HandlerDef) $1.attrs().get(play.api.routing.Router$Attrs$.MODULE$.HandlerDef()).get();" +
                                    "    controller = handlerRef.controller();" +
                                    "    action = handlerRef.method();" +
                                    "}" +
                                    "scala.Option spanKeyOption = $1.headers().get(\"" + X_SPAN_KEY + "\");" +  //spanKey to look up span for reporting action/controller used by metrics
                                    "String spanKey = spanKeyOption.isEmpty() ? null : (String)spanKeyOption.get();" +
                                    CLASS_NAME + ".layerEntry(spanKey, controller, action, $2);"
                            , false);
                } else {
                    insertBefore(method,
                            "scala.Option spanKeyOption = $1.headers().get(\"" + X_SPAN_KEY + "\");" +  //spanKey to look up span for reporting action/controller used by metrics
                                    "String spanKey = spanKeyOption.isEmpty() ? null : (String)spanKeyOption.get();" +
                                    CLASS_NAME + ".layerEntry(spanKey, null, null, $2);"
                            , false);
                }

                //add a catch block as for synchronous error, the exception get thrown right the way instead of going through Future
                CtClass exceptionType = classPool.get(Throwable.class.getName());
                method.addCatch("{ "
                        + CLASS_NAME + ".layerExitOnSynchronousException($e);"
                        + "throw $e; }", exceptionType);

                insertAfter(method,
                        Scope.class.getName() + " scope = " + CLASS_NAME + ".getAndCloseCurrentScope();"
                                + "    if (scope != null && $_ instanceof scala.concurrent.Future && " + ScalaUtil.EXECUTION_CONTEXT_FIELD + " != null) { "
                                +          Span.class.getName() + " span = scope.span();"
                                + "        Object successFunctionInstance = " + CLASS_NAME + ".getSuccessFunctionInstance(span);"
                                + "        Object failureFunctionInstance = " + CLASS_NAME + ".getFailureFunctionInstance(span);"
                                + "        if (successFunctionInstance instanceof scala.Function1 && failureFunctionInstance instanceof scala.Function1) {"
                                + "            $_  = $_.transform((scala.Function1)successFunctionInstance, (scala.Function1)failureFunctionInstance, " + ScalaUtil.EXECUTION_CONTEXT_FIELD + "); "
                                + "        }"
                                + "    }"
                        , true);
            }
        }

        return true;
    }

    private static boolean shouldStartSpan() {
        Integer currentDepth = functionDepthThreadLocal.get();
        if (currentDepth == null) {
            functionDepthThreadLocal.set(1);
            return true;
        } else {
            functionDepthThreadLocal.set(currentDepth + 1);
            return false;
        }
    }

    public static boolean shouldPatchCurrentCall() {
        Integer currentDepth = functionDepthThreadLocal.get();
        if (currentDepth == null) { //error, this code should not run when there's no entry
            logger.warn("Unexpected null depth on shouldPatchReturnFunction exit");
            return false;
        } else if (currentDepth > 1) {
            functionDepthThreadLocal.set(currentDepth - 1);
            return false;
        } else { //should be 1
            functionDepthThreadLocal.remove();
            return true;
        }
    }

    public static Scope getAndCloseCurrentScope() {
        if (shouldPatchCurrentCall()) {
            Scope playScope = ScopeManager.INSTANCE.active();
            if (playScope != null) {
                playScope.close(); //scope closes here as the current method call is exiting. Though this should NOT close the span as the span is exited when the Future completes
            }
            return playScope;
        } else {
            return null;
        }
    }

    public static void clearContext() {
        Context.clearMetadata();
    }

    public static Object getSuccessFunctionInstance(Span span) {
        if (successFunctionConstructor != null) {
            try {
                return successFunctionConstructor.newInstance(span);
            } catch (Exception e) {
                logger.warn("Failed to create a success function instance to track play layer exit");
            }
        }
        return null;
    }

    public static Object getFailureFunctionInstance(Span span) {
        if (failureFunctionConstructor != null) {
            try {
                return failureFunctionConstructor.newInstance(span);
            } catch (Exception e) {
                logger.warn("Failed to create a failure function instance to track play layer exit");
            }
        }
        return null;
    }

    private Class<?> createSuccessFunctionClass() throws CannotCompileException, NotFoundException, ClassNotFoundException {
        String callbackClassSimpleName = PlayScalaActionProfileInstrumentation.class.getSimpleName() + "SuccessFunction";
        FunctionClassHelper helper = FunctionClassHelper.getInstance(classPool, "scala.runtime.AbstractFunction1", callbackClassSimpleName);

        CtClass callbackClass = helper.getFunctionCtClass();
        callbackClass.addField(CtField.make("private " + Span.class.getName() + " span;", callbackClass));
        callbackClass.addMethod(CtNewMethod.make("public Object apply(Object obj) { "
                + CLASS_NAME + ".layerExit(span, null);"
                + "span = null;"
                + "    return obj; "
                + "}", callbackClass));
        callbackClass.addConstructor(CtNewConstructor.make("public " + callbackClassSimpleName + "(" + Span.class.getName() + " span) { "
                + "this.span = span; "
                + "}", callbackClass));
        return helper.toFunctionClass();
    }

    private Class<?> createFailureFunctionClass() throws CannotCompileException, NotFoundException, ClassNotFoundException {
        String callbackClassSimpleName = PlayScalaActionProfileInstrumentation.class.getSimpleName() + "FailureFunction";
        FunctionClassHelper helper = FunctionClassHelper.getInstance(classPool, "scala.runtime.AbstractFunction1", callbackClassSimpleName);

        CtClass callbackClass = helper.getFunctionCtClass();
        callbackClass.addField(CtField.make("private " + Span.class.getName() + " span;", callbackClass));
        callbackClass.addMethod(CtNewMethod.make("public Object apply(Object throwable) { "
                + CLASS_NAME + ".layerExit(span, throwable);"
                + "span = null;"
                + "    return throwable; "
                + "}", callbackClass));
        callbackClass.addConstructor(CtNewConstructor.make("public " + callbackClassSimpleName + "(" + Span.class.getName() + " span) { "
                + "this.span = span; "
                + "}", callbackClass));
        return helper.toFunctionClass();
    }



    public static void layerEntry(String spanKey, String controllerName, String actionName, Object block) {
        ProfileInfo info;
        if (controllerName != null || actionName != null) {
            info = buildProfileInfo(controllerName, actionName);
        } else {
            String blockName = block instanceof PlayBlockWrapper ? ((PlayBlockWrapper)block).getWrappedBlock().getClass().getName() : block.getClass().getName();
            info = extractProfileInfoFromBlockName(blockName);
        }

        if (spanKey != null) {
            Span span = SpanDictionary.getSpan(Long.valueOf(spanKey));
            if (span != null) {
                if (info.controllerName != null) {
                    span.setTracePropertyValue(TraceProperty.CONTROLLER, info.controllerName);
                }
                if (info.actionName != null) {
                    span.setTracePropertyValue(TraceProperty.ACTION, info.actionName);
                }

                if (span.context().isSampled() && shouldStartSpan()) {
                    Context.setMetadata(new Metadata(span.context().getMetadata())); //creates a clone for fork. Preserving the behavior before span conversion

                    Map<String, Object> keyValues = new HashMap<String, Object>();
                    keyValues.put("Language", "scala");
                    //keyValues.put("Class", blockName);
                    keyValues.put("ProfileName", info.profileName);
                    keyValues.put("FunctionName", "apply");
                    keyValues.put("Controller", info.controllerName);
                    if (info.actionName != null) {
                        keyValues.put("Action", info.actionName);
                    }

                    Scope playScope = Tracer.INSTANCE.buildSpan(LAYER_NAME).withReporters(TraceEventSpanReporter.REPORTER).withTags(keyValues).asChildOf(span).startActive(false);
                } else {
                    Context.setMetadata(span.context().getMetadata()); //just propagate the non-tracing context
                }
            } else {
                logger.warn("Cannot locate span from Play with key " + spanKey);
            }
        } else {
            logger.warn("Cannot locate span key from Play");
        }
    }

    public static void layerExitOnSynchronousException(Throwable exception) {
        Scope playScope = ScopeManager.INSTANCE.active();
        if (playScope != null) {
            layerExit(playScope.span(), exception);
            //do not close the scope here as it's closed in the finally clause inserted
        }

    }

    public static void layerExit(Span span, Object throwableObject) {
        if (span != null) {
            if (throwableObject instanceof Throwable) {
                reportError(span, (Throwable)throwableObject);
            }
            span.finish();
        }
    }


    private static ProfileInfo extractProfileInfoFromBlockName(String blockClassName) {
        //the action block name is in the form of:
        //For action as def : <Controller Name>$$anonfun$<Method name>$...
        //For action as val : <Controller Name>$$anonfun$<number>
        //Take note for action for val, we cannot find the action/method name as
        //it is hard (probably impossible with javassist) to parse the field name from the block that is assigned to it (take note that we have the block information, it is at the passing
        //the block within execution logic. It's not within the scope of instrumentation injection (with access to javassist) anymore.

        String startMarker = "$$anonfun$";
        String endMarker = "$";
        int startMarkerIndex = blockClassName.indexOf(startMarker);
        String controllerName;
        String actionName;


        if (startMarkerIndex != -1) {
            controllerName = blockClassName.substring(0, startMarkerIndex);

            String trailingString = blockClassName.substring(startMarkerIndex + startMarker.length());
            int endMarkerIndex = trailingString.indexOf(endMarker);
            if (endMarkerIndex != -1) {
                actionName =  trailingString.substring(0, endMarkerIndex);
            } else {
                actionName = null;
            }
        } else {
            controllerName = blockClassName; //worst scenario, cannot parse it, simply use the block name as controllerName
            actionName = null;
        }

        return buildProfileInfo(controllerName, actionName);
    }

    private static ProfileInfo buildProfileInfo(String controllerName, String actionName) {
        String profileName;
        if (actionName != null) {
            profileName = controllerName + "." + actionName;
        } else {
            profileName = controllerName;
        }

        return new ProfileInfo(controllerName, actionName, profileName);
    }

    private static class ProfileInfo {
        public ProfileInfo(String controllerName, String actionName, String profileName) {
            super();
            this.controllerName = controllerName;
            this.actionName = actionName;
            this.profileName = profileName;
        }
        private String controllerName;
        private String actionName;
        private String profileName;
    }



    private enum OpType {
        INVOKE_BLOCK
    }
}