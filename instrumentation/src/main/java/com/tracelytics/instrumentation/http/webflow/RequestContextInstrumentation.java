package com.tracelytics.instrumentation.http.webflow;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.ext.javassist.CtNewMethod;
import com.tracelytics.instrumentation.MethodSignature;

/**
 * Wrappers the <code>org.springframework.webflow.execution.RequestContext</code> with method that provides the current event id of the request context
 */
public class RequestContextInstrumentation extends BaseWebflowInstrumentation {
    public static final String CLASS_NAME = RequestContextInstrumentation.class.getName();
    
    //signatures of get Event method
    MethodSignature getTransitionMethods[] = {
        new MethodSignature("getCurrentTransition", "()Lorg/springframework/webflow/definition/TransitionDefinition;"),
    };
    
    
    MethodSignature getCurrentStateMethods[] = {
        new MethodSignature("getCurrentState", "()Lorg/springframework/webflow/definition/StateDefinition;"),
    };
    

    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {

        // Find and verify the get Event method exists
        CtMethod getTransitionMethod = findMethod(cc, getTransitionMethods);
        
        if (getTransitionMethod != null) {
            CtMethod getTransitionOnMethod = CtNewMethod.make("public String getTransitionOn() { " +
                                                         "    Object currentTransition = getCurrentTransition();" +
            		                                     "    if (currentTransition != null && currentTransition instanceof org.springframework.webflow.engine.Transition) {" +
            		                                     "        Object criteria = ((org.springframework.webflow.engine.Transition)currentTransition).getMatchingCriteria();" +
            		                                     "        if (criteria != null) {" +
         /*
          * For Decision State, current web-flow internally translates it into 2 evaluations. First it evaluates against the expression in the "if-clause",
          * if it is a match (returns true), then it will enter the state in "then-clause". If it fails the first evaluation (ie. returns false), it will then use
          * a catch-all wildcard expression ('*') that always matches, and map to the state in the "else-clause"
          * 
          * For details, please refer to <code>org.springframework.webflow.engine.builder.model.FlowModelFlowBuilder</code> and <code>org.springframework.webflow.engine.WildcardTransitionCriteria</code>
          *       
          * For decision state, mapped event identifiers spec, please refer to http://static.springsource.org/spring-webflow/docs/2.0.x/reference/htmlsingle/spring-webflow-reference.html#decision-state
          * 
          */
            		                                     "            boolean isFromDecisionState = false;" +
                                                         "            if (currentTransition instanceof com.tracelytics.instrumentation.http.webflow.Transition) {" +
                                                         "                isFromDecisionState = ((com.tracelytics.instrumentation.http.webflow.Transition)currentTransition).isFromDecisionState();" +
                                                         "            }" +
                                                         "" +
                                                         "            if (isFromDecisionState) {" +
                                                         "                return (criteria instanceof org.springframework.webflow.engine.WildcardTransitionCriteria) ? \"no\" : \"yes\";" +
                                                         "            } else {" +
                                                         "                return criteria.toString();" +
                                                         "            }" +
            		                                     "        }" +
            		                                     "    }" +
            		                                     "    return null;" +
            		                                     "}", cc);
            cc.addMethod(getTransitionOnMethod);
        } else {
            return false; //do not attempt to make any other modification as we cannot properly provide getEventId method
        }
        
        // Find and verify the get Current state method exists
        CtMethod getCurrentStateMethod = findMethod(cc, getCurrentStateMethods);
        
        if (getCurrentStateMethod != null) {
            CtMethod getCurrentStateClassStringMethod = CtNewMethod.make( "public Object getCurrentStateObject() { return " + getCurrentStateMethod.getName() + "(); }", cc);
            cc.addMethod(getCurrentStateClassStringMethod);
        } else {
            return false; //do not attempt to make any other modification as we cannot properly provide getCurrentStateObject method
        }
        
        
        // Add our interface so we can access the modified class in callbacks
        CtClass iface = classPool.getCtClass(RequestContext.class.getName());

        for (CtClass i : cc.getInterfaces()) {
            if (i.equals(iface)) {
                return true;
            }
        }
        
        cc.addInterface(iface);
        return true; 
    }


    
    
}



