package com.tracelytics.instrumentation.cache.spymemcached;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.tracelytics.ext.javassist.CannotCompileException;
import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtField;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.ext.javassist.CtNewMethod;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.MethodMatcher;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.Event;
import com.tracelytics.joboe.Metadata;
import com.tracelytics.joboe.OboeException;

/**
 * Instrumentation for Spy memcached client callback. We detect the complete method invocation in the callback and treat that as the end of 
 * asynchronous processing.
 * 
 * @author Patson Luk
 *
 */
public class SpyMemcachedCallbackInstrumentation extends ClassInstrumentation {
    private static final String LAYER_NAME = "spymemcached";
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<Integer>> gotDataMethodMatchers = Arrays.asList(
                                                                         new MethodMatcher<Integer>("gotData", new String[]{ "java.lang.String", "int", "long", "byte[]" }, "void", 4),
                                                                         new MethodMatcher<Integer>("gotData", new String[]{ "java.lang.String", "int", "byte[]" }, "void", 3));
    
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
            throws Exception {
        
        addTvContextObjectAware(cc);
        
        cc.addField(CtField.make("private String tvStatusString;", cc));
        cc.addMethod(CtNewMethod.make("public void setTvStatusString(String statusString) { tvStatusString = statusString;}", cc));
        cc.addMethod(CtNewMethod.make("public String getTvStatusString() { return tvStatusString; }", cc));
        
        cc.addField(CtField.make("private java.util.Map tvValueLengths = new java.util.concurrent.ConcurrentHashMap();", cc)); //since a single callback might handle multiple operations, we need to keep track of the size with a map
        cc.addMethod(CtNewMethod.make("public java.util.Map getTvValueLengths() { return tvValueLengths; }", cc));
        
        tagInterface(cc, SpyMemcachedCallback.class.getName());
        
        
        
        CtMethod completeMethod = cc.getMethod("complete", "()V");
        if (shouldModify(cc, completeMethod)) {
            insertAfter(completeMethod, 
                        SpyMemcachedCallbackInstrumentation.class.getName() + ".layerExit(this);",
                        true, 
                        false);
        }
        
        CtMethod receivedStatusMethod = cc.getMethod("receivedStatus", "(Lnet/spy/memcached/ops/OperationStatus;)V");
        if (shouldModify(cc, receivedStatusMethod)) {
            insertBefore(receivedStatusMethod, 
                    "if ($1 != null) { setTvStatusString($1.toString()); }");
        }
        
        instrumentGotData(cc);
        
        return true;
    }

    /**
     * Catches the gotData Method in order to record what long is the return value
     * @param cc
     */
    private void instrumentGotData(CtClass cc) {
        Map<CtMethod, Integer> gotDataMethods = findMatchingMethods(cc, gotDataMethodMatchers);
        
        for (Entry<CtMethod, Integer> gotDataMethodEntry : gotDataMethods.entrySet()) {
            if (shouldModify(cc, gotDataMethodEntry.getKey())) {
                String paramToken = "$" + gotDataMethodEntry.getValue();
                try {
                    insertBefore(gotDataMethodEntry.getKey(), 
                                 SpyMemcachedCallbackInstrumentation.class.getName() + ".recordDataSize(getTvValueLengths(), getTvContext(), " + paramToken + " != null ? " + paramToken + ".length : 0);");
                } catch (CannotCompileException e) {
                    logger.warn(e.getMessage(), e);
                }
            }
        }
        
    }
    
    public static void layerExit(final Object callbackObj) {
        if (!(callbackObj instanceof SpyMemcachedCallback)) {
            logger.warn("Spymemcached callback of class [" + callbackObj.getClass().getName() + "] was not tagged as [" + SpyMemcachedCallback.class.getName() + "]");
            return;
        }
        
        SpyMemcachedCallback callback = (SpyMemcachedCallback) callbackObj;
        
        Metadata metadata;
        synchronized(callbackObj) { //to avoid race condition for concurrent calls of complete on the same callback
            metadata = callback.getTvContext();
            //clear the context in the callback so other invocation would not create extra events to the same entry event
            callback.setTvContext(null);
        }
        
        if (metadata != null) {
            String statusString = callback.getTvStatusString();
            Map<Metadata, Integer> valueLengths = callback.getTvValueLengths();
        
            
            //In most cases the callback should be called within the IO processing thread (async process), but in case of timeout/cancel, the callback
            //is called right in the main thread. In order to correctly construct the graph, we do not want to update the context after this event is fired
            //Therefore we should store the context if it's valid in order to restore it after Event.report()
            Metadata previousContext = null;
            if (Context.getMetadata().isValid()) {
                previousContext = Context.getMetadata();
            }
            
            Context.setMetadata(metadata);
            Event event = Context.createEvent();
            event.addInfo("Layer", LAYER_NAME,
                          "Label", "exit");
            
            if (statusString != null) {
                event.addInfo("Status", statusString);
            }
            
            if (valueLengths.containsKey(metadata)) { //retrieve the length recorded by the gotData call(s)
                event.addInfo("ValueLength", valueLengths.get(metadata));
                valueLengths.remove(metadata);
            }
    
            event.setAsync();
            event.report();
            
            if (previousContext != null) {
                //restore the previous context if valid
                Context.setMetadata(previousContext);
            }
        }
    }
    
    /**
     * Records the accumulated value length based on the context. Each callback can be called multiple times (for multi-get operations) as can be shared
     * by multiple operations. We use the context as the key of the map as each of the Operation would have its unique context when the operation was first submitted
     * with the async layer entry event 
     * @param valueLengths
     * @param context
     * @param size
     */
    public static void recordDataSize(Map<Metadata, Integer> valueLengths, Metadata context, int size) {
        if (context != null && size > 0) {
            if (valueLengths.containsKey(context)) {
                valueLengths.put(context, valueLengths.get(context) + size);
            } else {
                valueLengths.put(context, size);
            }
        } 
    }
    
}