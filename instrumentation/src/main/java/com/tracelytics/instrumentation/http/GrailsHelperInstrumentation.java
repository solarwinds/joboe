package com.tracelytics.instrumentation.http;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.tracelytics.ext.javassist.CannotCompileException;
import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.ext.javassist.NotFoundException;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.MethodMatcher;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.Event;
import com.tracelytics.joboe.span.impl.ScopeManager;
import com.tracelytics.joboe.span.impl.Span;
import com.tracelytics.joboe.span.impl.Span.TraceProperty;

/**
 * Instruments Grails Controller operations by capturing operations in org.codehaus.groovy.grails.web.servlet.mvc.AbstractGrailsControllerHelper
 * 
 * From the executeAction, we can capture the controller class name, action and view name. This is to add extra info to the existing Grails extent
 * 
 * From the handleAction, we capture the actual Grails Controller action as profile 
 * 
 * 
 * @author pluk
 *
 */
public class GrailsHelperInstrumentation extends ClassInstrumentation {
    private static final String CLASS_NAME = GrailsHelperInstrumentation.class.getName();
    private static final String LAYER_NAME = "grails-controller";
    
    private static final ThreadLocal<String> currentAction = new ThreadLocal<String>();
           
            
    // List of method matchers that declare method with signature patterns that we want to instrument
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<OpType>> opMethods = Arrays.asList(
        new MethodMatcher<OpType>("handleAction", new String[]{ "groovy.lang.GroovyObject", "java.lang.Object" }, "java.lang.Object", OpType.HANDLE),
        new MethodMatcher<OpType>("executeAction", new String[]{ "groovy.lang.GroovyObject", "java.lang.String", "java.lang.String" }, "org.springframework.web.servlet.ModelAndView", OpType.EXECUTE),
        new MethodMatcher<OpType>("executeAction", new String[]{ "groovy.lang.GroovyObject", "org.codehaus.groovy.grails.commons.GrailsControllerClass", "java.lang.String" }, "org.springframework.web.servlet.ModelAndView", OpType.EXECUTE_OLD)
        
    );

    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
            throws Exception {
        applyOpInstrumentation(cc, opMethods);
        
        return true;
    }

    private void applyOpInstrumentation(CtClass cc, List<MethodMatcher<OpType>> methodMatchers) throws CannotCompileException, NotFoundException {
        
        Map<CtMethod, OpType> matchingMethods = findMatchingMethods(cc, methodMatchers);
        
        for (Entry<CtMethod, OpType> matchingMethodEntry : matchingMethods.entrySet()) {
            if (matchingMethodEntry.getValue() == OpType.HANDLE) {
                insertBefore(matchingMethodEntry.getKey(), CLASS_NAME + ".profileEntry($1, $2);");
                insertAfter(matchingMethodEntry.getKey(), CLASS_NAME + ".profileExit();", true);
            } else if (matchingMethodEntry.getValue() == OpType.EXECUTE) {
                insertBefore(matchingMethodEntry.getKey(), CLASS_NAME + ".reportInfo($1, $2, $3);", false);
                insertAfter(matchingMethodEntry.getKey(), CLASS_NAME + ".clearInfo();", true);
            } else if (matchingMethodEntry.getValue() == OpType.EXECUTE_OLD) {
                try {
                    cc.getField("actionName");
                    insertBefore(matchingMethodEntry.getKey(), CLASS_NAME + ".reportInfo($1, actionName, $3);", false);
                    insertAfter(matchingMethodEntry.getKey(), CLASS_NAME + ".clearInfo();", true);
                } catch (NotFoundException e) {
                    logger.warn("Cannot find actionName field, some of the Grails info will be missing");
                }
            }
        }
        
    }

    public static void profileEntry(Object controller, Object action) {
        Event event = Context.createEvent();
        event.addInfo("Layer", LAYER_NAME,
                      "Label", "entry");
        
        if (controller != null) {
            event.addInfo("Class", controller.getClass().getName());
        }
        
        String actionName;
        if (action instanceof Method) { //use method name if available
            Method actionMethod = ((Method)action);
            actionName = actionMethod.getName();
            
        } else if (currentAction.get() != null ) { //otherwise use the action name
            actionName = currentAction.get();
        } else {
            actionName = null;
            logger.warn("action object is not instance of [" + Method.class.getName() + "] nor with active currentAction!");
        }
        
        if (actionName != null) {
            event.addInfo("FunctionName", actionName);
        }
        
        event.report();
    }
    
    public static void profileExit() {
        Event event = Context.createEvent();
        event.addInfo("Layer", LAYER_NAME,
                      "Label", "exit");
        
        event.report();
    }

    public static void reportInfo(Object controllerObject, String actionName, String viewName) {
        if (controllerObject != null || actionName != null || viewName != null) {
            if (Context.getMetadata().isSampled()) {
                Event event = Context.createEvent();
                event.addInfo("Label", "info");
                
                if (controllerObject != null) {
                    event.addInfo("Controller", controllerObject.getClass().getName());
                    
                }
                
                if (actionName != null) {
                    event.addInfo("Action", actionName);
                    //store the action name action object later on might not contain the method names/action names anymore
                    currentAction.set(actionName);
                }
                
                if (viewName != null) {
                    event.addInfo("View", viewName);
                }
                event.report();
            }
            
            Span span = ScopeManager.INSTANCE.activeSpan();
            if (span != null && controllerObject != null) {
                span.setTracePropertyValue(TraceProperty.CONTROLLER, controllerObject.getClass().getName());
            }
            
            if (span != null && actionName != null) {
                span.setTracePropertyValue(TraceProperty.ACTION, actionName);
            }
        }
    }
    
    public static void clearInfo() {
        currentAction.remove();
    }
    
    private enum OpType {
        HANDLE, EXECUTE, EXECUTE_OLD
    }
}