package com.tracelytics.instrumentation.http.ws.server;

import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtField;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.ext.javassist.CtNewMethod;
import com.tracelytics.ext.javassist.NotFoundException;
import com.tracelytics.instrumentation.MethodMatcher;

/**
 * Instruments `org.glassfish.jersey.server.model.ResourceMethodInvoker` to form better transaction name by using the URI template
 * 
 * Also reports the actual java method invoked as Action and its declaring class as Controller
 * 
 * @author Patson
 *
 */
public class GlassfishJerseyMethodInvokerInstrumentation extends BaseRestServerInstrumentation {
    private static final String CLASS_NAME = GlassfishJerseyMethodInvokerInstrumentation.class.getName();
    // List of method matchers that declare method with signature patterns that we want to instrument
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<OpType>> methodMatchers = Arrays.asList(
            new MethodMatcher<OpType>("apply", new String[] { "org.glassfish.jersey.server.internal.process.RequestProcessingContext" }, "java.lang.Object", OpType.APPLY),
            new MethodMatcher<OpType>("apply", new String[] { "org.glassfish.jersey.server.ContainerRequest" }, "java.lang.Object", OpType.APPLY));
    
    private enum OpType {
        APPLY
    }
    
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes) throws Exception {
        if (!hasGetResourceMethod(cc)) {
            logger.warn("Failed to instrument [" + cc.getName() + "] as no getResourceMethod is found");
            return false;
        }
        
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
                    "String transactionName = tvGetTransactionName(getResourceMethod());" +
                            "if (transactionName != null) {" +
                                CLASS_NAME + ".reportTransactionName(transactionName);" +
                                CLASS_NAME + ".reportControllerAction(getResourceMethod());" +
                            "} else {" + //framework default handling if transactionName cannot be resolved
                                CLASS_NAME + ".reportInvokedMethod(getResourceMethod());" + //while the class and method name of framework default handling could still be useful, we should not treat it as controller/action
                            "}", false);

        }
        return true;
    }

    private static boolean hasGetResourceMethod(CtClass cc) {
        try {
            cc.getMethod("getResourceMethod", "()Ljava/lang/reflect/Method;");
            return true;
        } catch (NotFoundException e) {
            return false;
        }
    }
}