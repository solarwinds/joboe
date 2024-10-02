package com.tracelytics.instrumentation.http.ws;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.instrumentation.MethodMatcher;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Instrumentation for Spring Rest Template clients. Take note that an additional code snippet was injected into org.springframework.http.client.ClientHttpRequest as well
 * in order to capture the x-trace id in the Http response header
 * @author Patson Luk
 *
 */
public class RestTemplateInstrumentation extends BaseWsClientInstrumentation {
    
    private static String LAYER_NAME = "rest_client_spring";

    private static String CLASS_NAME = RestTemplateInstrumentation.class.getName();

    private enum MethodType { DO_EXECUTE }

    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<MethodType>> methodMatchers = Arrays.asList(new MethodMatcher<MethodType>("doExecute", new String[] { "java.net.URI", "org.springframework.http.HttpMethod" }, "java.lang.Object", MethodType.DO_EXECUTE));


    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {

        Set<CtMethod> methods = findMatchingMethods(cc, methodMatchers).keySet();
        
        for (CtMethod doExecuteMethod : methods) {
            insertBefore(doExecuteMethod, CLASS_NAME + ".layerEntryRest($2 != null ? $2.name() : null, $1 != null ? $1.toString() : null, \"" + LAYER_NAME + "\");");
            insertAfter(doExecuteMethod, CLASS_NAME + ".layerExitRest(\"" + LAYER_NAME + "\");", true);
        }

        return !methods.isEmpty();
    }
}