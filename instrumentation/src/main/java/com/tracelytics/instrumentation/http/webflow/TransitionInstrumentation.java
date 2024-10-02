package com.tracelytics.instrumentation.http.webflow;

import com.tracelytics.ext.javassist.CannotCompileException;
import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtField;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.ext.javassist.CtNewMethod;
import com.tracelytics.ext.javassist.Modifier;
import com.tracelytics.instrumentation.MethodSignature;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.Event;

/**
 * Instrumentation on Transition in order to track method/evaluation triggered by <code>org.springframework.webflow.engine.DecisionState</code>.
 * 
 * This is a special case for Action in DecisionState as it does not get wrapped and executed by <code>org.springframework.webflow.execution.ActionExecutor</code>
 * like other Actions
 * 
 * @author Patson Luk
 *
 */
public class TransitionInstrumentation extends BaseWebflowInstrumentation {

    public static final String CLASS_NAME = TransitionInstrumentation.class.getName();
    
    private static final String IS_FROM_DECISION_STATE_FIELD = "isFromDecisionState";
    
    // Signatures for "matches" methods
    private static MethodSignature[] matchesMethods = {
        new MethodSignature("matches", "(Lorg/springframework/webflow/execution/RequestContext;)Z"),
    };
    
    private static MethodSignature[] getMatchingCriteriaMethods = {
        new MethodSignature("getMatchingCriteria", "()Lorg/springframework/webflow/engine/TransitionCriteria;"),
    };
    
    private static MethodSignature[] executeMethods = {
        new MethodSignature("execute", "(Lorg/springframework/webflow/engine/State;Lorg/springframework/webflow/engine/RequestControlContext;)Z"),
    };
    
    
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {

        boolean modified = false;
        
        CtMethod matchesMethod = findMethod(cc, matchesMethods);

            
        if (matchesMethod != null && matchesMethod.getDeclaringClass().equals(cc)) {
            modifyMatchesMethod(matchesMethod);
            modified = true;
        }
        
        
        CtMethod getMatchingCriteriaMethod = findMethod(cc, getMatchingCriteriaMethods);
        
        if (matchesMethod != null) {
            CtMethod getMatchingCriteriaStringMethod = CtNewMethod.make("public String getMatchingCriteriaString() { " +
                    "    return " + getMatchingCriteriaMethod.getName() + "() == null ? null : " + getMatchingCriteriaMethod.getName() + "().toString();" +
                    "}", cc);
            
            cc.addMethod(getMatchingCriteriaStringMethod);
            
            
            modified = true;
        }
        
        
        CtMethod executeMethod = findMethod(cc, executeMethods);
        
        if (executeMethod != null) {
            //Add a field so we track whether last source state is Decision state.
            CtField f = new CtField(CtClass.booleanType, IS_FROM_DECISION_STATE_FIELD, cc); 
            f.setModifiers(Modifier.PRIVATE);
            cc.addField(f);
            
            insertBefore(executeMethod, CLASS_NAME + ".startExecute(this, $1);");
                       
            CtMethod setFromDecisionStateMethod = CtNewMethod.make("public void setFromDecisionState(boolean isFromDecisionState) { " +
                    "    this." + IS_FROM_DECISION_STATE_FIELD + " = isFromDecisionState;" + 
                    "}", cc);
            
            cc.addMethod(setFromDecisionStateMethod);
            
            
            CtMethod isFromDecisionStateMethod = CtNewMethod.make("public boolean isFromDecisionState() { " +
                    "    return " + IS_FROM_DECISION_STATE_FIELD + ";" + 
                    "}", cc);
            
            cc.addMethod(isFromDecisionStateMethod);
            
            modified = true;
        }
        
        CtClass iface = classPool.getCtClass(Transition.class.getName());
        
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
        
        
        return modified;
    }

   
    /**
     * Modifies the 'matches' method to add callbacks into our instrumentation code
     * @param method
     * @throws CannotCompileException
     */
    private void modifyMatchesMethod(CtMethod method)
            throws CannotCompileException {

        insertBefore(method, CLASS_NAME + ".trackMatches(this, $1, true);");
        insertAfter(method, CLASS_NAME + ".trackMatches(this, $1, false);", true);
    }
 
    /**
     * Track the start/end of an evaluation executed by this transition. We only track if the current State is 'Decision State' 
     * 
     * @param action
     * @param isStart
     */
    public static void trackMatches(Object transitionObject, Object requestContext, boolean isStart) {
        if (requestContext != null && requestContext instanceof RequestContext &&
            transitionObject != null && transitionObject instanceof Transition) {
            Transition transition = (Transition)transitionObject;
            
            Object currentState = ((RequestContext)requestContext).getCurrentStateObject();
            if (currentState != null && currentState instanceof DecisionState && transition.getMatchingCriteriaString() != null) {
                Event event = Context.createEvent();
                
              //TODO: Code temporarily commented out, trying Profile Event instead of Info Event
//                event.addInfo("Layer", LAYER_NAME,
//                              "Label", "info",
//                              "Type", isStart ? "Start an Action" : "Finish an Action",
//                              "ActionInfo", transition.getMatchingCriteriaString());
                
                event.addInfo("Layer", LAYER_NAME + "-matches",
                              "Language", "java",
                              "Label", isStart ? "entry" : "exit");
                
                if (isStart) {
                    event.addInfo("Expression", transition.getMatchingCriteriaString());
                    if (currentState instanceof State) {
                        event.addInfo("FlowId", ((State)currentState).getFlowId());
                        event.addInfo("StateId", ((State)currentState).getId());
                    }
                }
                
                event.report();
            }
        }
    }
    
    /**
     * Check whether the source state argument of the execute method is a Decision State, and store the result in the boolean
     * @param transition
     * @param sourceState
     */
    public static void startExecute(Object transition, Object sourceState) {
        ((Transition)transition).setFromDecisionState(sourceState != null && sourceState instanceof DecisionState);
    }
}



