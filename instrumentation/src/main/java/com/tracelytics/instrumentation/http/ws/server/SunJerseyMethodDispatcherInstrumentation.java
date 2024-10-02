package com.tracelytics.instrumentation.http.ws.server;

import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtField;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.ext.javassist.CtNewMethod;
import com.tracelytics.instrumentation.MethodMatcher;

/**
 * Instruments `com.sun.jersey.server.impl.model.method.dispatch.ResourceJavaMethodDispatcher` to form better transaction name by using the URI template
 * 
 * Also reports the actual java method invoked as Action and its declaring class as Controller
 * 
 * @author Patson
 *
 */
public class SunJerseyMethodDispatcherInstrumentation extends BaseRestServerInstrumentation {
    private static final String CLASS_NAME = SunJerseyMethodDispatcherInstrumentation.class.getName();
    // List of method matchers that declare method with signature patterns that we want to instrument
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<OpType>> methodMatchers = Arrays.asList(
            new MethodMatcher<OpType>("dispatch", new String[] { }, "void", OpType.DISPATCH));
    
    
    
    private enum OpType {
        DISPATCH
    }
    
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes) throws Exception {
        cc.addField(CtField.make("private boolean tvHasSetTransactionName = false;", cc));
        cc.addField(CtField.make("private String tvTransactionName = null;", cc));
        cc.addMethod(CtNewMethod.make("private String tvGetTransactionName(java.lang.reflect.Method method) {"
                + "    if (!tvHasSetTransactionName) {"
                + "        tvTransactionName = " + RestUtil.class.getName() + ".buildTransactionName(method);"
                + "        tvHasSetTransactionName = true;"
                + "    }"
                + "    return tvTransactionName;"
                + "}", cc));
        
        for (Entry<CtMethod, OpType> entry : findMatchingMethods(cc, methodMatchers).entrySet()) {
            insertBefore(entry.getKey(),
                    "String transactionName = tvGetTransactionName(this.method);" +
                            "if (transactionName != null) {" +
                                CLASS_NAME + ".reportTransactionName(transactionName);" +
                                CLASS_NAME + ".reportControllerAction(this.method);" +
                            "} else {" + //framework default handling if transactionName cannot be resolved
                                CLASS_NAME + ".reportInvokedMethod(this.method);" + //while the class and method name of framework default handling could still be useful, we should not treat it as controller/action
                            "}", false);
            
        }
        return true;
    }
}