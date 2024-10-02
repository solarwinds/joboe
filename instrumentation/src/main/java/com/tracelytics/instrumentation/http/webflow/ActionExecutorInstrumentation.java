package com.tracelytics.instrumentation.http.webflow;

import com.tracelytics.ext.javassist.CannotCompileException;
import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.instrumentation.MethodSignature;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.Event;

/**
 * Instrumentation on Action defined in the Webflow XML. Take note that most of the actions defined in the XML will get wrapped by Webflow into 
 * <code>org.springframework.webflow.execution.Action</code> and go through the <code>org.springframework.webflow.execution.ActionExecutor</code>.
 * 
 * However, one exception is that in Decision State the evaluation is passed down to Spring evaluation directly w/o going through the executor
 * 
 * @author Patson Luk
 *
 */
public class ActionExecutorInstrumentation extends BaseWebflowInstrumentation {

    public static final String CLASS_NAME = ActionExecutorInstrumentation.class.getName();
    
    // Signatures for "execute" methods
    private static MethodSignature[] executeMethods = {
        new MethodSignature("execute", "(Lorg/springframework/webflow/execution/Action;Lorg/springframework/webflow/execution/RequestContext;)Lorg/springframework/webflow/execution/Event;"),
    };
    
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {

        boolean modified = false;
        
        CtMethod executeMethod = findMethod(cc, executeMethods);

            
        if (executeMethod != null && executeMethod.getDeclaringClass().equals(cc)) {
            modifyExecuteMethod(executeMethod);
            modified = true;
        }
        
        return modified;
    }

   
    /**
     * Modify the execute method to add callbacks into our instrumentation code
     * @param method
     * @throws CannotCompileException
     */
    private void modifyExecuteMethod(CtMethod method)
            throws CannotCompileException {

        insertBefore(method, CLASS_NAME + ".trackExecute($1, $2, true);");
        insertAfter(method, CLASS_NAME + ".trackExecute($1, $2, false);", true);
    }
 
    /**
     * Track the start/end of an Action executed by the executor
     * @param action
     * @param isStart
     */
    public static void trackExecute(Object action, Object requestContextObject, boolean isStart) {
        Event event = Context.createEvent();

        event.addInfo("Layer", LAYER_NAME + "-action",
                      "Language", "java",
                      "Label", isStart ? "entry" : "exit");
        
        if (isStart) {
            String functionName;
            if (action instanceof AnnotatedAction) {
                functionName = ((AnnotatedAction)action).getTargetActionString(); //remove the AnnotatedAction wrapper for simplicity
            } else if (action instanceof ExpressionAction) {
                functionName = ((ExpressionAction)action).getExpressionString(); //A known Action type, display the expression string directly
            } else {
                functionName = action.toString(); //unknown action type, just use the toString() method
            }
            
            if (requestContextObject != null && requestContextObject instanceof RequestContext) {
                Object currentState = ((RequestContext)requestContextObject).getCurrentStateObject();
                if (currentState != null && currentState instanceof State) {
                    event.addInfo("FlowId", ((State)currentState).getFlowId());
                    event.addInfo("StateId", ((State)currentState).getId());
                }
            }
            event.addInfo("FunctionName", functionName);
        }
        
        event.report();
    }
}



