package com.tracelytics.instrumentation.http.play;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.tracelytics.ext.javassist.CannotCompileException;
import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.MethodMatcher;
import com.tracelytics.instrumentation.Module;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.Event;

/**
 * Instruments Play 2 template (html response) rendering. Take note that we prefer to capture the "apply" method as the "render" method is not invoked in Scala template flow.
 * @author pluk
 *
 */
public class PlayTemplateInstrumentation extends ClassInstrumentation {
    private static String LAYER_NAME_PLAY = "play-template";
    private static String LAYER_NAME_TWIRL = "twirl-template";
    private static String CLASS_NAME = PlayTemplateInstrumentation.class.getName();
    private static ThreadLocal<Integer> depthThreadLocal = new ThreadLocal<Integer>() {
        protected Integer initialValue() {
            return 0;
        }
    };
    
    // List of method matchers that declare method with signature patterns that we want to instrument
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<OpType>> playMethodMatchers = Arrays.asList(
        new MethodMatcher<OpType>("render", new String[] { }, "play.api.templates.Html", OpType.RENDER), //Play 2.0 - 2.2 template
        new MethodMatcher<OpType>("apply", new String[] { }, "play.api.templates.Html", OpType.APPLY), //Play 2.0 - 2.2 template
        new MethodMatcher<OpType>("render", new String[] { }, "play.twirl.api.Html", OpType.RENDER), //Play 2.3+ template
        new MethodMatcher<OpType>("apply", new String[] { }, "play.twirl.api.Html", OpType.APPLY) //Play 2.3+ template
    );
    
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<OpType>> twirlMethodMatchers = Arrays.asList(
        new MethodMatcher<OpType>("render", new String[] { }, "twirl.api.Html", OpType.RENDER), //twirl (standalone) template
        new MethodMatcher<OpType>("apply", new String[] { }, "twirl.api.Html", OpType.APPLY) //twirl (standalone) template
    );
    
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {
        applyInstrumentationByFrameworkType(cc, playMethodMatchers, LAYER_NAME_PLAY);
        applyInstrumentationByFrameworkType(cc, twirlMethodMatchers, LAYER_NAME_TWIRL);
       
        return true;
    }
    
    /**
     * Play has its template in 2.2 and was renamed to "Twirl" since version 2.3. There are other frameworks that use "Twirl" directly, and those might 
     * NOT have "play" in its package name, but other than that everything else is essentially the same 
     * 
     * @param cc
     * @param methodMatchers
     * @param layerName
     * @throws CannotCompileException
     */
    private void applyInstrumentationByFrameworkType(CtClass cc, List<MethodMatcher<OpType>> methodMatchers, String layerName) throws CannotCompileException {
        Map<CtMethod, OpType> methodEntries =  findMatchingMethods(cc, methodMatchers);
        for (Entry<CtMethod, OpType> methodEntry : methodEntries.entrySet()) {
            CtMethod method = methodEntry.getKey();
            
            if (shouldModify(cc, method)) {
                OpType opType = methodEntry.getValue();
                if (opType.equals(OpType.APPLY)) {
                    insertBefore(method, CLASS_NAME + ".profileEntry(\"" + cc.getName() + "\", \"" + method.getName() + "\", \"" + layerName + "\");");
                    insertAfter(method,  CLASS_NAME + ".profileExit(\"" + cc.getName() + "\", \"" + layerName + "\");", true);
                } else if (opType.equals(OpType.RENDER) && !methodEntries.containsValue(OpType.APPLY)) { //only instrument RENDER if APPLY is not found
                    insertBefore(method, CLASS_NAME + ".profileEntry(\"" + cc.getName() + "\", \"" + method.getName() + "\", \"" + layerName + "\");");
                    insertAfter(method,  CLASS_NAME + ".profileExit(\"" + cc.getName() + "\", \"" + layerName + "\");", true);
                }
            }
            
        }
    }
    
    
    public static void profileEntry(String templateName, String methodName, String layerName) {
        Event entryEvent = Context.createEvent();
        entryEvent.addInfo("Layer", layerName,
                           "Label", "entry",
                           "Language", "java");
            
        if (templateName.endsWith("$")) { //Scala object
            templateName = templateName.substring(0, templateName.length() - 1);
        }
        
        entryEvent.addInfo("Class", templateName);
        entryEvent.addInfo("FunctionName", methodName);
        
        addBackTrace(entryEvent, 1, Module.PLAY);
        
        entryEvent.report();
    }

    public static void profileExit(String templateName, String layerName) {
        Event exitEvent = Context.createEvent();
        exitEvent.addInfo("Layer", layerName,
                           "Label", "exit");
        exitEvent.report();                               
    }
    
    

    
    private enum OpType {
        RENDER, APPLY
    }
}