package com.tracelytics.instrumentation.actor.akka;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.tracelytics.ext.google.common.collect.MapMaker;
import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.MethodMatcher;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.Metadata;

/**
 * Keep track of the <code>akka.dispatch.Envelope</code> objects by using a map with the envelope as key and <code>AkkaEnvelopeInfo</code> in order to:
 * <ol>
 *  <li>Keep track of a context paired with the Envelope such that <code>AkkaActorCellInstrumentation</code> can restore it at the running actor thread</li>
 *  <li>Track the creation time to calculate the wait time in the mailbox</li>
 *  <li>Carry a flag to indicate whether this is an active extent created for processing this envelope</li>
 * </ol>
 * 
 * 
 * Take note that we cannot modify the <code>akka.dispatch.Envelope</code> class directly as it conflicts with Kamon's instrumentation, more details in 
 * https://github.com/tracelytics/joboe/issues/439
 * 
 * @author pluk
 *
 */
public class AkkaEnvelopeCompanionInstrumentation extends ClassInstrumentation {

    private static String CLASS_NAME = AkkaEnvelopeCompanionInstrumentation.class.getName();
    
    
    private enum OpType { APPLY }
    
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<OpType>> methodMatchers = Arrays.asList(
         new MethodMatcher<OpType>("apply", new String[] {}, "akka.dispatch.Envelope", OpType.APPLY)
    ); 
    
    //has to use guava weak hash map as we want a weak hashmap that does Identity check (not object equality) as 2 Envelops (different instances)
    //considered the same if they have same contents (scala case class), but we want the map based on the identity not the envelope content (such
    //that 2 envelop instances with the same contents are considered different keys in the map 
    private static Map<Object, AkkaEnvelopeInfo> envelopeInfoMap = new MapMaker().weakKeys().makeMap();
    
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
            throws Exception {
        
        for (CtMethod method : findMatchingMethods(cc, methodMatchers).keySet()) {
            insertAfter(method, CLASS_NAME + ".registerEnvelope($_);", true, false);
        }
        return true;
    }
    
    /**
     * Registers the envelopObject by creating a coupling AkkaEnvelopeInfo object
     * @param envelopeObject
     */
    public static void registerEnvelope(Object envelopeObject) {
        if (envelopeObject != null && Context.getMetadata().isValid()) {
            AkkaEnvelopeInfo info = new AkkaEnvelopeInfo();
            //clone the context, as the original context might get cleared (for example Spray) before akka has a chance to exit, and we want to avoid that
            Metadata clonedContext = new Metadata(Context.getMetadata()); 
            info.setTvContext(clonedContext);
            info.setTvFromThreadId(Thread.currentThread().getId());
            info.setTvRestored(false);
            
            envelopeInfoMap.put(envelopeObject, info);
        }
    }
    
    static AkkaEnvelopeInfo getEnvelopeInfo(Object envelopeObject) {
        return envelopeInfoMap.get(envelopeObject);
    }
    
    static AkkaEnvelopeInfo unregisterEnvelope(Object envelopeObject) {
        return envelopeInfoMap.remove(envelopeObject);
    }
}

    
