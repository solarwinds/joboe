package com.tracelytics.instrumentation.http.play;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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
import com.tracelytics.joboe.Event;
import com.tracelytics.joboe.Metadata;
import com.tracelytics.joboe.span.impl.Span;
import com.tracelytics.joboe.span.impl.Span.TraceProperty;
import com.tracelytics.joboe.span.impl.SpanDictionary;

/**
 * Instruments the Play 2 Action and profile with Java template. This takes similar approach as documented in {@link PlayScalaActionProfileInstrumentation}
 * @author pluk
 *
 */
public class PlayJavaActionProfileInstrumentation extends PlayBaseInstrumentation {

    private static String CLASS_NAME = PlayJavaActionProfileInstrumentation.class.getName();
    
    private static Constructor<?> successFunctionConstructor;
    private static Constructor<?> failureFunctionConstructor;
    private static Constructor<?> failure2_1FunctionConstructor;
    
    private enum OpType {
        APPLY, APPLY_FUTURE
    }
    
    // List of method matchers that declare method with signature patterns that we want to instrument
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<OpType>> methodMatchers = Arrays.asList(
                                                                      new MethodMatcher<OpType>("apply", new String[] { "play.api.mvc.Request"}, "play.api.mvc.Result", OpType.APPLY), //2.0 - 2.1
                                                                      new MethodMatcher<OpType>("apply", new String[] { "play.api.mvc.Request"}, "scala.concurrent.Future", OpType.APPLY_FUTURE) //2.2+
                                                                      );
    


    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
            throws Exception {
            
        Map<CtMethod, OpType> matchingMethods = findMatchingMethods(cc, methodMatchers);
        Version playVersion = identifyVersion();
        if (playVersion.isNewerOrEqual(Version.PLAY_2_1)) {
            synchronized(PlayJavaActionProfileInstrumentation.class) {
                if (playVersion.isNewerOrEqual(Version.PLAY_2_2)) {
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
                } else if (playVersion == Version.PLAY_2_1) {
                    if (successFunctionConstructor == null) {
                        Class<?> mapperClass = createSuccessFunctionClass();
                        if (mapperClass != null && mapperClass.getDeclaredConstructors().length > 0) {
                            successFunctionConstructor = mapperClass.getDeclaredConstructors()[0];
                        }
                    }
                    if (failure2_1FunctionConstructor == null) { 
                        Class<?> mapperClass = createFailure2_1FunctionClass();
                        if (mapperClass != null && mapperClass.getDeclaredConstructors().length > 0) {
                            failure2_1FunctionConstructor = mapperClass.getDeclaredConstructors()[0];
                        }
                    }
                }
            }
            ScalaUtil.addScalaContextForInstrumentation(cc);
        }
        
        
        
        
        for (Entry<CtMethod, OpType> methodEntry : matchingMethods.entrySet()) {
            CtMethod method = methodEntry.getKey();
            OpType type = methodEntry.getValue();
                    
            if (type == OpType.APPLY_FUTURE) {
                insertBefore(method,
                                "scala.Option spanKeyOption = $1.headers().get(\"" + X_SPAN_KEY + "\");" + //restore the context from header, we need this as the Play action runs on it's own thread
                                "String spanKey = spanKeyOption.isEmpty() ? null : (String)spanKeyOption.get();" +
                                 CLASS_NAME + ".layerEntry(spanKey, annotations() != null ? annotations().controller() : null, annotations() != null ? annotations().method() : null);"
                                , false);
                 
                
                addErrorReporting(method, Throwable.class.getName(), LAYER_NAME, classPool);
                
                 
                insertAfter(method, 
                "if ($_ instanceof scala.concurrent.Future) { "
                + "Object successFunctionInstance = " + CLASS_NAME + ".getSuccessFunctionInstance();"
                + "Object failureFunctionInstance = " + CLASS_NAME + ".getFailureFunctionInstance();"
                + "if (successFunctionInstance instanceof scala.Function1 && failureFunctionInstance instanceof scala.Function1) {"
                + "    $_  = ($r)$_.transform((scala.Function1)successFunctionInstance, (scala.Function1)failureFunctionInstance, " + ScalaUtil.EXECUTION_CONTEXT_FIELD + "); "
                + "}"
              + "}"
              + Context.class.getName() + ".clearMetadata();" //need to clear context of current thread, see https://github.com/librato/joboe/issues/577
              , true);
            } else if (type == OpType.APPLY) {
                insertBefore(method,
                        "scala.Option spanKeyOption = $1.headers().get(\"" + X_SPAN_KEY + "\");" + //spanKey to look up span for reporting action/controller used by metrics 
                        "String spanKey = spanKeyOption.isEmpty() ? null : (String)spanKeyOption.get();" + 
                         CLASS_NAME + ".layerEntry(spanKey, controller(), method());"
                        , false);
         
        
                addErrorReporting(method, Throwable.class.getName(), LAYER_NAME, classPool);
                
                if (playVersion == Version.PLAY_2_1) { //in 2.1 java, it might return play.api.mvc.AsyncResult instead of a plain result, if that's the case, we need to use callback to capture the actual duration
                    insertAfter(method, 
                            "if ($_ instanceof play.api.mvc.AsyncResult) {"
                          + "    Object successFunctionInstance = " + CLASS_NAME + ".getSuccessFunctionInstance();"
                          + "    Object failureFunctionInstance = " + CLASS_NAME + ".getFailure2_1FunctionInstance();"
                          + "    if (successFunctionInstance instanceof scala.Function1 && failureFunctionInstance instanceof scala.PartialFunction) {"
                          + "        $_ = ($r)((play.api.mvc.AsyncResult)$_).transform((scala.Function1)successFunctionInstance, " + ScalaUtil.EXECUTION_CONTEXT_FIELD + ");" //for 2.1 there's only one transform, so only success would get caught here
                          + "        if (((play.api.mvc.AsyncResult)$_).result() != null) {"
                          + "            ((play.api.mvc.AsyncResult)$_).result().onFailure((scala.PartialFunction)failureFunctionInstance, " + ScalaUtil.EXECUTION_CONTEXT_FIELD + ");" //for failure use the onFailure. This might exit after the trace does, but at least we would not have broken traces
                          + "        }"
                          + "    }"
                          +      Context.class.getName() + ".clearMetadata();" //need to clear context of current thread, see https://github.com/librato/joboe/issues/577
                          + "} else {"
                          +      CLASS_NAME + ".layerExit(" + Context.class.getName() + ".getMetadata(), null);"
                          + "}", true);
                } else {
                    insertAfter(method, CLASS_NAME + ".layerExit(" + Context.class.getName() + ".getMetadata(), null);", true);
                }
            }
        }
        return true;
    }

    public static void layerEntry(String spanKey, Class<?> controllerClass, Method method) {
        if (spanKey != null) {
            Span span = SpanDictionary.getSpan(Long.valueOf(spanKey));
            if (span != null) {
                if (span.context().isSampled()) {
                    Context.setMetadata(new Metadata(span.context().getMetadata())); //create a clone for fork. Preserving the behavior before span conversion
                    
                  //layer entry
                    Event event = Context.createEvent();
                    
                    event.addInfo("Layer", LAYER_NAME,
                                  "Label", "entry",
                                  "Language", "java");
                    if (method != null) {
                      event.addInfo("Action", method.getName());
                    }
                    
                    if (controllerClass != null) {
                      event.addInfo("Controller", controllerClass.getName());
                    }
                    
                    event.report();
                } else {
                    Context.setMetadata(span.context().getMetadata()); //just propagate the non-tracing context 
                }
                
                if (controllerClass != null && method != null) {
                    span.setTracePropertyValue(TraceProperty.CONTROLLER, controllerClass.getName());
                    span.setTracePropertyValue(TraceProperty.ACTION, method.getName());
                }
            } else {
                logger.warn("Cannot locate span from Play with key " + spanKey);
            }
        } else {
            logger.warn("Cannot locate span key from Play");
        }
    }
    
      
    
    public static void clearContext() {
        Context.clearMetadata();
    }
    
    public static void layerExit(Metadata context, Object throwable) {
        if (context != null && context.isSampled()) { //only create an exit event if there was a valid context to start the layer
            Context.setMetadata(context);
            
            if (throwable instanceof Throwable) {
                reportError(LAYER_NAME, (Throwable) throwable);
            }
            
            //layer exit
            Event event = Context.createEvent();
            
            event.addInfo("Layer", LAYER_NAME,
                          "Label", "exit");
            
            event.report();
        }
        
        Context.clearMetadata();
    }
    
    public static Object getSuccessFunctionInstance() {
        if (successFunctionConstructor != null) {
            try {
                return successFunctionConstructor.newInstance(Context.getMetadata());
            } catch (Exception e) {
                logger.warn("Failed to create a success function instance to track play layer exit");
            } 
        }
        return null;
    }
    
    public static Object getFailureFunctionInstance() {
        if (failureFunctionConstructor != null) {
            try {
                return failureFunctionConstructor.newInstance(Context.getMetadata());
            } catch (Exception e) {
                logger.warn("Failed to create a failure function instance to track play layer exit");
            } 
        }
        return null;
    }
    
    public static Object getFailure2_1FunctionInstance() {
        if (failure2_1FunctionConstructor != null) {
            try {
                return failure2_1FunctionConstructor.newInstance(Context.getMetadata());
            } catch (Exception e) {
                logger.warn("Failed to create a failure partial function instance to track play 2.1 java layer exit");
            } 
        }
        return null;
    }
    
    private Class<?> createSuccessFunctionClass() throws CannotCompileException, NotFoundException, ClassNotFoundException {
        String callbackClassSimpleName = PlayJavaActionProfileInstrumentation.class.getSimpleName() + "SuccessFunction";
        FunctionClassHelper helper = FunctionClassHelper.getInstance(classPool, "scala.runtime.AbstractFunction1", callbackClassSimpleName);

        CtClass callbackClass = helper.getFunctionCtClass();
        callbackClass.addField(CtField.make("private " + Metadata.class.getName() + " context;", callbackClass));
        callbackClass.addMethod(CtNewMethod.make("public Object apply(Object obj) throws Throwable { "
                + CLASS_NAME + ".layerExit(context, null);"
                + "context = null;"
           + "    return obj; "
           + "}", callbackClass));
        callbackClass.addConstructor(CtNewConstructor.make("public " + callbackClassSimpleName + "(" + Metadata.class.getName() + " forkedContext) { "
                + "context = forkedContext; "
                + "}", callbackClass));
        return helper.toFunctionClass();
    }
    
    private Class<?> createFailureFunctionClass() throws CannotCompileException, NotFoundException, ClassNotFoundException {
        String callbackClassSimpleName = PlayJavaActionProfileInstrumentation.class.getSimpleName() + "FailureFunction";
        FunctionClassHelper helper = FunctionClassHelper.getInstance(classPool, "scala.runtime.AbstractFunction1", callbackClassSimpleName);

        CtClass callbackClass = helper.getFunctionCtClass();
        callbackClass.addField(CtField.make("private " + Metadata.class.getName() + " context;", callbackClass));
        callbackClass.addMethod(CtNewMethod.make("public Object apply(Object throwable) throws Throwable { "
                + CLASS_NAME + ".layerExit(context, throwable);"
                + "context = null;"
           + "if (throwable instanceof Throwable) {"
           + "    throw (Throwable)throwable; "
           + "} else {"
           + "    return throwable;" //unexpected
           + "}"
           + "}", callbackClass));
        callbackClass.addConstructor(CtNewConstructor.make("public " + callbackClassSimpleName + "(" + Metadata.class.getName() + " forkedContext) { "
                + "context = forkedContext; "
                + "}", callbackClass));
        return helper.toFunctionClass();
    }
    
    /**
     * 2.1 we can only call the onFailure of the result() of play.api.mvc.AsyncResult.result(). onFailure takes a partial function
     * @return
     * @throws CannotCompileException
     * @throws NotFoundException
     */
    private Class<?> createFailure2_1FunctionClass() throws CannotCompileException, NotFoundException {
        final String CALLBACK_CLASS_NAME = CLASS_NAME + "$PlayJava2_1FailureFunction";
        CtClass callbackClass = classPool.makeClass(CALLBACK_CLASS_NAME);
        callbackClass.setSuperclass(classPool.get("scala.runtime.AbstractPartialFunction"));
        callbackClass.addField(CtField.make("private " + Metadata.class.getName() + " context;", callbackClass));
        callbackClass.addMethod(CtNewMethod.make("public boolean isDefinedAt(Object obj) { return true; } ",callbackClass));
        callbackClass.addMethod(CtNewMethod.make("public Object apply(Object throwable) throws Throwable { "
                + CLASS_NAME + ".layerExit(context, throwable);"
                + "context = null;"
           + "if (throwable instanceof Throwable) {"
           + "    throw (Throwable)throwable; "
           + "} else {"
           + "    return throwable;" //unexpected
           + "}"
           + "}", callbackClass));
        callbackClass.addConstructor(CtNewConstructor.make("public PlayJava2_1FailureFunction(" + Metadata.class.getName() + " forkedContext) { "
                + "context = forkedContext; "
                + "}", callbackClass));
        return callbackClass.toClass(PlayJavaActionProfileInstrumentation.class);
    }

    public static String getProfileName(String typeName, String actionName) {
        return typeName + "." + actionName;
    }
}