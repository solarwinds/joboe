package com.tracelytics.instrumentation.http.webflow;

import java.util.Arrays;
import java.util.List;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtConstructor;
import com.tracelytics.ext.javassist.CtField;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.ext.javassist.CtNewMethod;
import com.tracelytics.ext.javassist.NotFoundException;
import com.tracelytics.instrumentation.ConstructorMatcher;

/**
 * Applies a wrapper to known action classes such as
 * <code>org.springframework.webflow.execution.AnnotatedAction</code> and <code>org.springframework.webflow.action.EvaluateAction</code>
 * to provide cleaner action information to end-user
 * 
 * @author Patson Luk
 *
 */
public class ActionInstrumentation extends BaseWebflowInstrumentation {
    @SuppressWarnings("unchecked")
    private static final List<ConstructorMatcher<Object>> EVALUATION_ACTION_CONSTRUCTORS = Arrays.asList(
        new ConstructorMatcher<Object>(new String[] {"org.springframework.binding.expression.Expression", "org.springframework.webflow.action.ActionResultExposer"}),
        new ConstructorMatcher<Object>(new String[] {"org.springframework.binding.expression.Expression", "org.springframework.binding.expression.Expression"}));
            
    

    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {

        boolean modified = false;
        
        //2 known action wrapper classes commonly used for Webflow
        CtClass annotatedActionClass;
        CtClass evaluateActionClass;
        CtClass setActionClass;
        
        try {
            annotatedActionClass = classPool.getCtClass("org.springframework.webflow.execution.AnnotatedAction");
            evaluateActionClass = classPool.getCtClass("org.springframework.webflow.action.EvaluateAction");
            setActionClass = classPool.getCtClass("org.springframework.webflow.action.SetAction");
        } catch (NotFoundException e) {
            logger.debug("Not instrumenting " + className + ": " + e.getMessage());
            return false;
        }
        
        if (annotatedActionClass.equals(cc)) {
            CtMethod getTargetActionStringMethod = CtNewMethod.make("public String getTargetActionString() { " +
                                                                      "    org.springframework.webflow.execution.Action action = getTargetAction();" +
                                                                      "    if (action != null) {" +
                                                                      "        if (action instanceof " + ExpressionAction.class.getName() + ") {" +
                                                                      "            return ((" + ExpressionAction.class.getName() + ")action).getExpressionString();" +
                                                                      "        } else {" +
                                                                      "            return action.toString();" +
                                                                      "        }" +
                                                                      "    }" +
                                                                      "    return null;" +
                                                                      "}", cc);
            cc.addMethod(getTargetActionStringMethod);

            CtClass iface = classPool.getCtClass(AnnotatedAction.class.getName());
            cc.addInterface(iface);
            
            modified = true;
        } else if (evaluateActionClass.equals(cc)) {
            cc.addField(CtField.make("private Object tvExpression;", cc));
            cc.addField(CtField.make("private Object tvResultExpression;", cc));
            for (CtConstructor constructor : findMatchingConstructors(cc, EVALUATION_ACTION_CONSTRUCTORS).keySet()) {
              //capture the fields in the ctor, as it's problematic to refer to private fields directly
                insertAfter(constructor, 
                            "tvExpression = $1; "
                          + "if ($2 instanceof " + ActionResultExposer.class.getName() + ") {" //if it's a ActionResultExposer, we use the result expression embedded in it instead
                          + "    tvResultExpression = ((" + ActionResultExposer.class.getName() + ")$2).tvGetExpression();"
                          + "} else {"
                          + "    tvResultExpression = $2;"
                          + "}"
                            , true
                            , false); //should not enforce context check as the EvaluateAction only get instantiated once and get reused, even if the request is not sampled, we should still set values here
            }
            
             
            
            
            CtMethod getExpressionStringMethod = CtNewMethod.make("public String getExpressionString() { " +
                                                                  "    if (tvResultExpression != null && tvExpression != null) {" +
                                                                  "        return tvResultExpression.toString() +  \" = \" + tvExpression.toString();" +
                                                                  "    } else if (tvExpression != null) {" +
                                                                  "        return tvExpression.toString();" +
                                                                  "    } else {" +
                                                                  "        return null;" +
                                                                  "    }" +
                                                                  "}", cc);
            cc.addMethod(getExpressionStringMethod);
            
            CtClass iface = classPool.getCtClass(ExpressionAction.class.getName());
            cc.addInterface(iface);
            
            modified = true;
        } else if (setActionClass.equals(cc)) {
            CtMethod getExpressionStringMethod = CtNewMethod.make("public String getExpressionString() { " +
                    "    return (nameExpression != null && valueExpression != null) ? (nameExpression.toString() + \" = \" + valueExpression.toString()): null;" +
                    "}", cc);
            cc.addMethod(getExpressionStringMethod);
            
            CtClass iface = classPool.getCtClass(ExpressionAction.class.getName());
            cc.addInterface(iface);
            
            modified = true;
        }
        
        return modified;
    }
}



