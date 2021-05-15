package com.tracelytics.instrumentation.http;

import com.tracelytics.ext.javassist.CannotCompileException;
import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.ext.javassist.expr.ExprEditor;
import com.tracelytics.ext.javassist.expr.MethodCall;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.MethodMatcher;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.Event;
import com.tracelytics.joboe.span.impl.ScopeManager;
import com.tracelytics.joboe.span.impl.Span;
import com.tracelytics.joboe.span.impl.Span.TraceProperty;

import java.util.Arrays;
import java.util.List;

/**
 * Instruments JSF Action Listener to extract controller/action.  The object's
 * class name is mapped to controller, while them method is mapped to action.
 *
 * http://www.docjar.com/html/api/com/sun/faces/application/ActionListenerImpl.java.html
 *
 * The action listener is what calls into the application code to figure out the next view.
 * to display.
 *
 */
public class JSFActionListenerInstrumentation extends ClassInstrumentation {
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<OpType>> methodMatchers = Arrays.asList(
            new MethodMatcher<OpType>("processAction", new String[] { "javax.faces.event.ActionEvent" }, "void", OpType.PROCESS, MethodMatcher.Strategy.STRICT),
            new MethodMatcher<OpType>("processAction", new String[] { "jakarta.faces.event.ActionEvent" }, "void", OpType.PROCESS, MethodMatcher.Strategy.STRICT)
    );

    private enum OpType { PROCESS }


    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
            throws Exception {

        for (CtMethod method : findMatchingMethods(cc, methodMatchers).keySet()) {
            modifyProcessActionMethod(method);
        }
       return true;
    }

    private void modifyProcessActionMethod(CtMethod method)
        throws CannotCompileException {

        // Edit the method to extract the action
        method.instrument(new ExprEditor() {

            public void edit(MethodCall m) throws CannotCompileException {
                boolean addedInfo= false;

                if (m.getClassName().equals("javax.faces.el.MethodBinding") || m.getClassName().equals("jakarta.faces.el.MethodBinding")) {
                    if (m.getMethodName().equals("invoke") && !addedInfo) {
                        // Before request is sent:
                        insertBeforeMethodCall(m, CLASS_NAME + ".doInvokeInfo($0.getExpressionString()); ", false);
                        addedInfo = true;
                    }
                }
            }
        });
    }

    /**
     * Adds controller/action info
     * @param expr  #{object.method}
     */
    public static void doInvokeInfo(String expr) {
        // Expr is generally of the syntax #{object.method}  However, more complex forms are allowed:
        // http://developers.sun.com/docs/jscreator/help/2update1/jsp-jsfel/jsf_expression_language_intro.html
        //
        // To keep this as generic as possible we ignore the { } delimiters, then take everything before the first
        // dot as a controller, and everything after as the action. If, for some reason, there is no dot we only
        // have a controller.

        if (expr == null) {
            return;
        }

        // Extract the controller / action:
        int startExpr = expr.indexOf("{");
        int endExpr = expr.indexOf("}");
        if (endExpr < startExpr || startExpr == -1 || endExpr == -1) {
            return;
        }

        String controller;
        String action;
        int dot = expr.indexOf('.');
        if (dot == -1) {
            controller = expr.substring(startExpr + 1, endExpr).trim();
            action = "";
        } else {
            controller = expr.substring(startExpr + 1, dot).trim();
            action = expr.substring(dot + 1, endExpr).trim();
        }

        if (Context.getMetadata().isSampled()) {
            // Report the event:
            Event event = Context.createEvent();
            event.addInfo("Layer", LAYER_NAME,
                          "Label", "info",
                          "Controller", controller,
                          "Action", action
                         );
            event.report();
        }
        
        Span span = ScopeManager.INSTANCE.activeSpan();
        if (span != null) {
            span.setTracePropertyValue(TraceProperty.CONTROLLER, controller);
            span.setTracePropertyValue(TraceProperty.ACTION, action);
        }
    }


    static String CLASS_NAME = JSFActionListenerInstrumentation.class.getName();
    static String LAYER_NAME = "jsf";
}