package com.tracelytics.instrumentation.nosql;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.instrumentation.MethodMatcher;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.Event;

/**
 * Instruments <code>org.apache.hadoop.hbase.client.BufferedMutator</code> which is used for batch operations since hbase client version 1.0.0
 * 
 * @author Patson Luk
 *
 */
public class HbaseBufferedMutatorInstrumentation extends HbaseBaseInstrumentation {
    private static final String CLASS_NAME = HbaseBufferedMutatorInstrumentation.class.getName();
            
    // List of method matchers that declare method with signature patterns that we want to instrument
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<MethodType>> opMethods = Arrays.asList(
        new MethodMatcher<MethodType>("mutate", new String[]{ "org.apache.hadoop.hbase.client.Mutation" }, "void", MethodType.MUTATE_ONE),
        new MethodMatcher<MethodType>("mutate", new String[]{ "java.util.List" }, "void", MethodType.MUTATE_LIST),
        new MethodMatcher<MethodType>("flush", new String[]{ }, "void", MethodType.FLUSH)
   );
    
    
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
            throws Exception {
        Map<CtMethod, MethodType> matchingMethods = findMatchingMethods(cc, opMethods);
        
        for (Entry<CtMethod, MethodType> matchingMethodEntry : matchingMethods.entrySet()) {
            CtMethod method = matchingMethodEntry.getKey();
            if (matchingMethodEntry.getValue() == MethodType.MUTATE_ONE) {
                insertBefore(method, CLASS_NAME + ".layerEntry(\"" + method.getName() + "\", getName() != null ? getName().getName() : (byte[])null, $1);");
            } else if (matchingMethodEntry.getValue() == MethodType.MUTATE_LIST) {
                insertBefore(method, CLASS_NAME + ".layerEntry(\"" + method.getName() + "\", getName() != null ? getName().getName() : (byte[])null, $1 != null ? $1.size() : 0);");
            } else {
                insertBefore(method, CLASS_NAME + ".layerEntry(\"" + method.getName() + "\", getName() != null ? getName().getName() : (byte[])null, null);");
            }
            insertAfter(method, CLASS_NAME + ".layerExit(\"" + method.getName() + "\");", true);
        }
        
        return true;
    }

    
    public static void layerEntry(String opName, byte[] tableName, Object operation) {
        layerEntry(opName, tableName, extractOperationInfo(operation));
    }
    
    public static void layerEntry(String opName, byte[] tableName, int opSize) {
        layerEntry(opName, tableName, Collections.singletonMap("OpSize", opSize));
    }
    
    public static void layerEntry(String opName, byte[] tableName, Map<String, ?> extraValues) {
        if (shouldStartExtent()) {
            Event event = Context.createEvent();
            event.addInfo("Layer", LAYER_NAME,
                          "Flavor", FLAVOR,
                          "Label", "entry",
                          "Query", opName);
            
            if (tableName != null) {
                try {
                    event.addInfo("Table", encodeTableName(tableName));
                } catch (UnsupportedEncodingException e) {
                    logger.warn("Cannot encode the HBase table name, " + e.getMessage());
                }
            }
            
            if (extraValues != null) {
                for (Entry<String, ?> keyValue : extraValues.entrySet()) {
                    if (keyValue.getValue() instanceof byte[]) { //special handling for byte array
                        if ("Qualifier".equals(keyValue.getKey())) { //do not report Qualifier as a separate value
                            continue;
                        }
                        byte[] valueAsByteArray = (byte[]) keyValue.getValue();
                        
                        if ("CF".equals(keyValue.getKey())) {
                            byte[] qualifier = (byte[]) extraValues.get("Qualifier");
                            if (qualifier == null) {
                                event.addInfo("CF", toStringBinary(valueAsByteArray));
                            } else {
                                event.addInfo("CF", toStringBinary(valueAsByteArray) + ":" + toStringBinary(qualifier));
                            }
                        } else {
                            event.addInfo(keyValue.getKey(), toStringBinary(valueAsByteArray));
                        }
                    } else {
                        event.addInfo(keyValue.getKey(), keyValue.getValue());
                    }
                }
            }
            
            event.report();
        }
    }
    

    public static void layerExit(String opName) {
        if (shouldEndExtent()) {
            Event event = Context.createEvent();
            event.addInfo("Layer", LAYER_NAME,
                          "Label", "exit",
                          "Query", opName);
            
            event.report();
        }
    }
    
    
    
    private enum MethodType {
        MUTATE_ONE, MUTATE_LIST, FLUSH;
    }
}