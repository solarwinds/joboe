package com.tracelytics.instrumentation.nosql.redis.lettuce;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.MethodMatcher;

/**
 * All of the Redis Lettuce operations actually go through {@code RedisAsyncConnection.dispatch}. However we do not want to start and end an extent there as it is an asynchronous operation
 * .
 * 
 * It is not suitable to tag context (for context restoration when the operation is executed in other thread) to the future object returned as in some case this is just the wrapper of the actual future.
 * And in other rare scenarios, the operation might even complete before the corresponding future is returned to the caller (hence too late for context tagging).
 * 
 * Therefore, we patch this class to indicate there is an active dispatch ongoing, such that lower level instrumentation {@link RedisLettuceCommandInstrumentation }
 * can inherit and tags to itself the context properly in the ctor .
 * 
 * 
 * Also we added logic to assist with script tracking in this class. In the common case (non script method calls), the key is recorded by instrumented setKey method done to CommandArgs
 * and reported in {@code RedisLettuceCommandHandlerInstrumentation}. However, script was either added by add(String value) or addValue(Object value) method which
 * both are commonly used for non script purpose. Therefore we cannot rely on instrumenting the CommandArgs directly. Instead, we can keep track of the method invocations
 * done to {@code RedisAsyncConnection}, if it is either eval or evalsha, we can store the script object temporarily and tag it to the CommandArgs when dispatch is invoked 
 * 
 * 
 * @author Patson Luk
 *
 */
public class RedisLettuceAsyncConnectionPatcher extends ClassInstrumentation {
    private static final String CLASS_NAME = RedisLettuceAsyncConnectionPatcher.class.getName();
    private static ThreadLocal<Object> scriptThreadLocal = new ThreadLocal<Object>(); //temporary store the script object during eval/evalsha call
    
    private enum OpType { DISPATCH, EVAL }
            
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<OpType>> methodMatchers = Arrays.asList(
        new MethodMatcher<OpType>("dispatch", new String[]{ "com.lambdaworks.redis.protocol.CommandType", "com.lambdaworks.redis.protocol.CommandOutput", "com.lambdaworks.redis.protocol.CommandArgs" }, "java.lang.Object", OpType.DISPATCH, true),
        new MethodMatcher<OpType>("eval", new String[]{ "java.lang.Object" }, "java.lang.Object", OpType.EVAL),
        new MethodMatcher<OpType>("evalsha", new String[]{ "java.lang.String" }, "java.lang.Object", OpType.EVAL)
    );
    
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
            throws Exception {
        Map<CtMethod, OpType> matchingMethods = findMatchingMethods(cc, methodMatchers);
        
        for (Entry<CtMethod, OpType> matchingMethodEntry : matchingMethods.entrySet()) {
            OpType type = matchingMethodEntry.getValue();
            CtMethod method = matchingMethodEntry.getKey();
            if (type == OpType.DISPATCH) {
                insertBefore(method, CLASS_NAME + ".dispatchEntry($3);");
                insertAfter(method, CLASS_NAME + ".dispatchExit();", true);
            } else if (type == OpType.EVAL) {
                insertBefore(method, CLASS_NAME + ".evalEntry($1);");
                insertAfter(method, CLASS_NAME + ".evalExit();", true);
            }
        }
        
        return true;
    }
    
    /**
     * Indicates there is an active dispatch, set the metadata. Also patches the script to the commandArgs if avaialable
     */
    public static void dispatchEntry(Object commandArgsObject) {
        if (scriptThreadLocal.get() != null && commandArgsObject instanceof RedisLettuceCommandArgs) {
            ((RedisLettuceCommandArgs)commandArgsObject).tvSetScript(scriptThreadLocal.get());
        }
        
        RedisLettuceContext.setActiveDispatch();
    }
    
    /**
     * Indicates there is no longer an active dispatch, unset the metadata
     */
    public static void dispatchExit() {
        RedisLettuceContext.unsetActiveDispatch();
    }
    
    /**
     * Temporary stores the script object so at the dispatch we can tag it to the commandArgs
     * @param script
     */
    public static void evalEntry(Object script) {
        scriptThreadLocal.set(script);
    }
    
    /**
     * Always removes the script object on eval/evalsha exit
     */
    public static void evalExit() {
        scriptThreadLocal.remove();
    }
}