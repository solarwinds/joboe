package com.tracelytics.instrumentation.nosql.redis.redisson;

import com.tracelytics.ext.javassist.ClassPool;
import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.ext.javassist.NotFoundException;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.ClassMap;
import com.tracelytics.instrumentation.FrameworkVersion;
import com.tracelytics.instrumentation.Module;
import com.tracelytics.joboe.span.impl.Scope;
import com.tracelytics.joboe.span.impl.ScopeManager;
import com.tracelytics.joboe.span.impl.Span.SpanProperty;
import com.tracelytics.joboe.span.impl.Tracer;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Instruments the "distribute" java object implemented by Redisson, this is the higher level object manipulation which get translated to 
 * lower level Redis Operation by Redisson and handled by Lettuce. The actual Redis operation is instrumented in Lettuce instead
 * 
 * @author Patson Luk
 *
 */
public abstract class BaseRedisRedissonObjectInstrumentation extends ClassInstrumentation {
    private static final String CLASS_NAME = BaseRedisRedissonObjectInstrumentation.class.getName();
    public static final String LAYER_NAME = "redis-redisson";
    
    //instead of listing all the methods from various RObject, we use a list of methods in interfaces that we want to instrument here
    //, then match them against the implementing RObject instead
    private final List<String> OBJECT_INTERFACE_NAMES = getTargetTypes();

    protected abstract List<String> getTargetTypes();

    private static ThreadLocal<Boolean> isIteratorOperation = new ThreadLocal<Boolean>();

    private static ThreadLocal<Integer> depthThreadLocal = new ThreadLocal<Integer>() {
        protected Integer initialValue() {
            return 0;
        }
    };

    private FrameworkVersion version = null;


    
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
            throws Exception {

        if (version == null) {
            version = new RedisRedissonVersionReader(classPool).getFrameworkVersion();
        }
        if (!isSupportedVersion(version)) {
            return false;
        }
        //find matching Iterator (nested/anonymous classes) as those should be patched to ensure `getAsync` operations spawned by `hasNext`
        //are marked as synchronous
        for (CtClass childIteratorClass : findChildIteratorClasses(cc)) {
            ClassMap.registerInstrumentation(childIteratorClass.getName(), RedisRedissonIteratorPatcher.class, Module.REDIS);
        }

        Set<CtClass> objectInterfaces = findMatchingObjectInterfaces(cc, OBJECT_INTERFACE_NAMES);

        //Collects all the methods declared in {@code OBJECT_INTERFACE_NAMES} as preparation for the matching follows
        Set<CtMethod> targetMethods = new HashSet<CtMethod>();
        for (CtClass interfaze : objectInterfaces) {
            targetMethods.addAll(Arrays.asList(interfaze.getDeclaredMethods()));
        }

        boolean hasGetNameMethod;
        try {
            cc.getMethod("getName", "()Ljava/lang/String;");
            hasGetNameMethod = true;
        } catch (NotFoundException e) {
            hasGetNameMethod = false;
        }

        //iterates through all declared methods, and find out the ones that matches the methods declared in interfaces listed in {@code SYNC_OBJECT_INTERFACE_NAMES}
        for (CtMethod method : cc.getDeclaredMethods()) {
            if ("getName".equals(method.getName())) { //do not instrument getName, otherwise we get into infinite loop as we use it in injected instrumentation
                continue;
            }
            if (targetMethods.contains(method)) {
                if (version.getMajorVersion() == 1 || !method.getName().endsWith("Async")) { //version 1.x, we do not have special handling for async operations
                    insertBefore(method, CLASS_NAME + ".layerEntry(\"" + method.getName() + "\", " + (hasGetNameMethod ? "getName()" : "null") + ", true);");
                    insertAfter(method, CLASS_NAME + ".layerExit();", true);
                } else {
                    insertBefore(method, CLASS_NAME + ".layerEntry(\"" + method.getName() + "\", " +  (hasGetNameMethod ? "getName()" : "null") + ", false);");
                    insertAfter(method, getAsyncHandling(), true);
                }
            }
        }
        
        
        return true;
    }

    protected abstract boolean isSupportedVersion(FrameworkVersion version);


    protected String getAsyncHandling() {
        return ""; //by default do nothing
    }


    public static void layerEntry(String methodName, String objectName, boolean synchronous) {
        if (shouldStartExtent()) {
            Tracer.SpanBuilder spanBuilder = buildTraceEventSpan(LAYER_NAME);

            if (methodName != null) {
                spanBuilder.withTag("MethodName", methodName);
            }
            if (objectName != null) {
                spanBuilder.withTag("ObjectName", objectName);
            }

            if (!synchronous && isIteratorOperation.get() == null) { //iterator operation is always synchronous
                spanBuilder.withSpanProperty(SpanProperty.IS_ASYNC, true);
            }

            spanBuilder.startActive(synchronous);
        }
    }

    
    public static void layerExit() {
        if (shouldEndExtent()) {
            Scope scope = ScopeManager.INSTANCE.active();
            if (scope != null && LAYER_NAME.equals(scope.span().getOperationName())) {
                scope.close();
            } else {
                logger.warn("Found mismatching scope, expect : ["+ LAYER_NAME + "] but found " + scope);
            }
        }
    }

    private Set<CtClass> findChildIteratorClasses(CtClass cc) throws NotFoundException {
        Set<CtClass> iteratorClasses = new HashSet<CtClass>();
        CtClass iteratorType = classPool.get("java.util.Iterator");
        for (CtClass nestedClass : cc.getNestedClasses()) {
            if (nestedClass.subtypeOf(iteratorType)) {
                    iteratorClasses.add(nestedClass);
            }
        }
        return iteratorClasses;
    }

    private Set<CtClass> findMatchingObjectInterfaces(CtClass cc, List<String> targetInterfaceNames) throws NotFoundException {
        Set<CtClass> matchingObjectInterfaces = new HashSet<CtClass>();
        
        for (CtClass interfaze : cc.getInterfaces()) {
            matchingObjectInterfaces.addAll(findMatchingObjectInterfaces(interfaze, targetInterfaceNames));
        }
        
        if (targetInterfaceNames.contains(cc.getName())) {
            matchingObjectInterfaces.add(cc);
        }
        
        return matchingObjectInterfaces;
    }
    
    /**
     * Checks whether the current instrumentation should start a new extent. If there is already an active extent, then do not start one
     * @return
     */
    public static boolean shouldStartExtent() {
        int currentDepth = depthThreadLocal.get();
        depthThreadLocal.set(currentDepth + 1);

        return currentDepth == 0;
    }

    /**
     * Checks whether the current instrumentation should end the current extent. If this is the active extent being traced, then ends it
     * @return
     */
    public static boolean shouldEndExtent() {
        int currentDepth = depthThreadLocal.get();
        depthThreadLocal.set(currentDepth - 1);

        return currentDepth == 1;
    }

    public static int getCurrentDepth() {
        return depthThreadLocal.get();
    }

    public static void flagIteratorOperation(boolean value) {
        if (value) {
            isIteratorOperation.set(true);
        } else {
            isIteratorOperation.remove();
        }
    }

}