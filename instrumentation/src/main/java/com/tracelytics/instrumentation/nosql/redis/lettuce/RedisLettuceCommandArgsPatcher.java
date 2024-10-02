package com.tracelytics.instrumentation.nosql.redis.lettuce;

import java.util.Arrays;
import java.util.List;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtField;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.ext.javassist.CtNewMethod;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.MethodMatcher;

/**
 * Patches the Lettuce {@code CommandArgs} such that we can more conveniently retrieve the key value set against the {@code CommandArgs} instance
 * @author Patson Luk
 *
 */
public class RedisLettuceCommandArgsPatcher extends ClassInstrumentation {
    private enum OpType { ADD_KEY }
    
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<OpType>> methodMatchers = Arrays.asList(
        new MethodMatcher<OpType>("addKey", new String[]{ "java.lang.Object" }, "com.lambdaworks.redis.protocol.CommandArgs", OpType.ADD_KEY, true)
    );
    
    
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
            throws Exception {
        cc.addField(CtField.make("private Object tvKey;", cc));    
        cc.addField(CtField.make("private Object tvScript;", cc));
        cc.addField(CtField.make("private int tvKeyCount;", cc));
        
        cc.addMethod(CtNewMethod.make("public void tvSetKey(Object keyObject) { " +
        		                      "    tvKeyCount++; " +
        		                      "    if (tvKeyCount == 1) { " + //only store the first key according to the KV spec
        		                      "        tvKey = keyObject;" +
        		                      "    }" +
        		                      "}", cc));
        
        cc.addMethod(CtNewMethod.make("public void tvSetScript(Object scriptObject) { " +
                                      "    tvScript = scriptObject;" +
                                      "}", cc));
        
        cc.addMethod(CtNewMethod.make("public Object tvGetKey() { return tvKey; }", cc));
        cc.addMethod(CtNewMethod.make("public Object tvGetScript() { return tvScript; }", cc));
        cc.addMethod(CtNewMethod.make("public int tvGetKeyCount() { return tvKeyCount; }", cc));
        
        tagInterface(cc, RedisLettuceCommandArgs.class.getName());
        
        for (CtMethod method : findMatchingMethods(cc, methodMatchers).keySet()) {
            insertBefore(method, "tvSetKey($1);"); //record the key when setKey is called
        }
        
        return true;
    }
}