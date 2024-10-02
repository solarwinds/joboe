package com.tracelytics.instrumentation.http.webflow;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.tracelytics.ext.javassist.CannotCompileException;
import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.ext.javassist.CtNewMethod;
import com.tracelytics.instrumentation.MethodSignature;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.Event;

/**
 * Instruments on <code>org.springframework.webflow.engine.State</code> and its child classes. Tracks state changes (state enter, exit or refresh) in order to 
 * provide information of the flow.
 * 
 * Take note that this also tracks the render event on <code>org.springframework.webflow.engine.ViewState</code>
 * 
 * @author Patson Luk
 *
 */
public class StateInstrumentation extends BaseWebflowInstrumentation {

    public static final String CLASS_NAME = StateInstrumentation.class.getName();
    
    // Signatures for "enter" methods in all states
    private static MethodSignature[] enterMethods = {
        new MethodSignature("enter", "(Lorg/springframework/webflow/engine/RequestControlContext;)V"),
        new MethodSignature("enter", "(Lorg/springframework/webflow/engine/RequestControlContext;)Lorg/springframework/webflow/execution/ViewSelection;") //webflow 1 method
    };
    
    // Signatures for "exit" methods, take note that only TransitionableState has exit methods
    private static MethodSignature[] exitMethods = {
        new MethodSignature("exit", "(Lorg/springframework/webflow/engine/RequestControlContext;)V"),
        new MethodSignature("exit", "(Lorg/springframework/webflow/engine/RequestControlContext;)Lorg/springframework/webflow/execution/ViewSelection;")  //webflow 1 method 
    };
    
    // Signatures for "refresh" methods in ViewState
    private static MethodSignature refreshMethodV1 = new MethodSignature("refresh", "(Lorg/springframework/webflow/execution/RequestContext;)Lorg/springframework/webflow/execution/ViewSelection;");  //webflow 1 method
    private static MethodSignature refreshMethodV2 = new MethodSignature("refresh", "(Lorg/springframework/webflow/execution/View;Lorg/springframework/webflow/engine/RequestControlContext;)V");
    
    // Signatures for "render" methods in ViewState
    private static MethodSignature[] renderMethods = {
        new MethodSignature("render", "(Lorg/springframework/webflow/engine/RequestControlContext;Lorg/springframework/webflow/execution/View;)V")
    };
    
    
    private static final Map<String, String> STATE_CLASS_MAPPING = initializeStateClassMapping();
    
    
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {

        boolean modified = false;
        
        //modify all states to track beginning of state enter
        CtMethod enterMethod = findMethod(cc, enterMethods);
            
        if (enterMethod != null && enterMethod.getDeclaringClass().equals(cc)) {
            modifyEnterMethod(enterMethod);
            modified = true;
        }
        
        //modify transitionable states to track beginning of state exit
        CtClass transitionableStateClass = classPool.getCtClass("org.springframework.webflow.engine.TransitionableState");
        
        if (transitionableStateClass.equals(cc)) {
            CtMethod exitMethod = findMethod(cc, exitMethods);
            if (exitMethod != null && exitMethod.getDeclaringClass().equals(cc)) {
                modifyExitMethod(exitMethod);
                modified = true;
            }
        }
        
        //modify view state to track render and state refresh
        CtClass viewStateClass = classPool.getCtClass("org.springframework.webflow.engine.ViewState");
        
        if (viewStateClass.equals(cc)) {
            CtMethod refreshMethod = findMethod(cc, refreshMethodV2);
            if (refreshMethod != null) {
                modifyRefreshMethodV2(refreshMethod);
                modified = true;
            }  else {
                refreshMethod = findMethod(cc, refreshMethodV1); //try webflow 1 signature
                if (refreshMethod != null) {
                    modifyRefreshMethodV1(refreshMethod);
                    modified = true;
                }
                
            }
            
            CtMethod renderMethod = findMethod(cc, renderMethods);
            if (renderMethod != null) {
                modifyRenderMethod(renderMethod);
                modified = true;
            }
        }
        
        //modify(tag) decision state so we can identify it later in <code>ActionExecutorInstrumentation</code>
        CtClass decisionStateClass = classPool.getCtClass("org.springframework.webflow.engine.DecisionState");
        
        if (decisionStateClass.equals(cc)) {
            CtClass iface = classPool.getCtClass(DecisionState.class.getName());

            boolean tagged = false;
            for(CtClass i : cc.getInterfaces()) {
                if (i.equals(iface)) {
                    tagged = true;
                    break; // already tagged
                }
            }
            
            if (!tagged) {
                cc.addInterface(iface);
            }
            
            modified = true;
        }
        
        
        if (modified) {
            CtMethod getFlowIdMethod = CtNewMethod.make( "public String getFlowId() { return getFlow() == null ? null : getFlow().getId(); }", cc);
            cc.addMethod(getFlowIdMethod);
            
            
            // Add our interface so we can access the modified class in callbacks
            CtClass iface = classPool.getCtClass(State.class.getName());

            for(CtClass i : cc.getInterfaces()) {
                if (i.equals(iface)) {
                    return true; // already tagged
                }
            }

            cc.addInterface(iface);
        }
        

        return modified;
    }
    
     /**
     * Modify the enter method to add callbacks into our instrumentation code
     * @param method
     * @throws CannotCompileException
     */
    private void modifyEnterMethod(CtMethod method)
            throws CannotCompileException {

        insertBefore(method, CLASS_NAME + ".startEnter(this, $1);");
    }
    
    /**
     * Modify the exit method to add callbacks into our instrumentation code
     * @param method
     * @throws CannotCompileException
     */
    private void modifyExitMethod(CtMethod method)
            throws CannotCompileException {

        insertBefore(method, CLASS_NAME + ".startExit(this, $1);");
    }
    
    /**
     * Modifies the refresh method of webflow 1
     * @param method
     * @throws CannotCompileException
     */
    private void modifyRefreshMethodV1(CtMethod method)
            throws CannotCompileException {
        insertBefore(method, CLASS_NAME + ".startRefresh(this, $1);");
    }
    
    /**
     * Modifies the refresh method of webflow 2
     * @param method
     * @throws CannotCompileException
     */
    private void modifyRefreshMethodV2(CtMethod method)
            throws CannotCompileException {
        insertBefore(method, CLASS_NAME + ".startRefresh(this, $2);");
    }
    
    private void modifyRenderMethod(CtMethod method) throws CannotCompileException {
        insertBefore(method, CLASS_NAME + ".startRender($2);");
    }
    
       
    /**
     * Instrumentation code called by (beginning of) entering a <code>State</code>
     * @param caller
     * @param requestContext
     */
    public static void startEnter(Object caller, Object requestContext) {
        startTrack(caller, requestContext, StateTrackType.ENTER);
    }
    
    /**
     * Instrumentation code called by (beginning of) refreshing a <code>State</code>. Take note that for refresh, we only track the "enter" of a refresh state" not the "exit"
     * @param caller
     * @param requestContext
     */
    public static void startRefresh(Object caller, Object requestContext) {
        startTrack(caller, requestContext, StateTrackType.REFRESH);
    }
    
    /**
     * Instrumentation code called by (beginning of) the exit process of a <code>TransitionableState</code>
     * @param caller
     * @param requestContext
     * @param isRefresh
     */
    public static void startExit(Object caller, Object requestContext) {
        startTrack(caller, requestContext, StateTrackType.EXIT);
    }
    
    private static void startTrack(Object caller, Object requestContext, StateTrackType trackType) {
        Event event = Context.createEvent();
        
        event.addInfo("Layer", LAYER_NAME,
                      "Label", "info");
                      
        
        if (trackType == StateTrackType.ENTER) {
            event.addInfo("Type", "Enter a State");
        } else if (trackType == StateTrackType.REFRESH) {
            event.addInfo("Type", "Refresh on a View State");
        } else if (trackType == StateTrackType.EXIT) {
            event.addInfo("Type", "Exit a State");
        } else {
            logger.warn("Unknown web-flow action track type: " + trackType);
        }
        
        
        
        if (requestContext != null && requestContext instanceof RequestContext) {
            String transitionOn = ((RequestContext)requestContext).getTransitionOn();
            
            if (transitionOn != null) {
                event.addInfo("TransitionOn", transitionOn);
            }
        }
        
        if (caller instanceof State) {
            String flowId = ((State)caller).getFlowId();
            String stateId = ((State)caller).getId();
            
            
            event.addInfo("FlowId", flowId,
                          "StateId", stateId);
            String stateType = findStateType(caller);
            
            if (stateType != null) {
                event.addInfo("StateType", stateType);
            }
        } else {
            logger.warn("Webflow state is not correctly tagged with " + State.class.getName() + " interface");
        }
        
        
        event.report();
    }
    
    
    private enum StateTrackType {
        ENTER, EXIT, REFRESH
    }
    
    
    /**
     * Instrument the render method of <code>ViewState</code> for view information such as view name and physical view path (jsp pages etc)
     * @param view
     */
    public static void startRender(Object view) {
        Event event = Context.createEvent();
        
        event.addInfo("Layer", LAYER_NAME,
                      "Label", "info",
                      "Type", "Start Rendering a View",
                      "View", view.toString());
        
        event.report();
    }
    
    
       
    private static Map<String, String> initializeStateClassMapping() {
        Map<String, String> mapping = new HashMap<String, String>();
        
        mapping.put("org.springframework.webflow.engine.EndState", "End State");
        mapping.put("org.springframework.webflow.engine.ActionState", "Action State");
        mapping.put("org.springframework.webflow.engine.DecisionState", "Decision State");
        mapping.put("org.springframework.webflow.engine.SubflowState", "Subflow State");
        mapping.put("org.springframework.webflow.engine.ViewState", "View State");
        
        return Collections.unmodifiableMap(mapping);
    }
    
    
    private static String findStateType(Object caller) {
        Class<? extends Object> classWalker = caller.getClass();
        
        while (classWalker != null) {
            String stateType = STATE_CLASS_MAPPING.get(classWalker.getName());
            
            if (stateType != null) { //found a matching state class
                return stateType;
            }
            
            classWalker = classWalker.getSuperclass();
        }
        
        return null;
    }
}



