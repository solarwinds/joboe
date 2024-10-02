package com.tracelytics.instrumentation.nosql;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtConstructor;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.ext.javassist.NotFoundException;
import com.tracelytics.instrumentation.ConstructorMatcher;
import com.tracelytics.instrumentation.MethodMatcher;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.Event;

/**
 * Instruments various operations done to HTable
 * 
 * @author Patson Luk
 *
 */
public class HbaseTableInstrumentation extends HbaseBaseInstrumentation {
    private static final String CLASS_NAME = HbaseTableInstrumentation.class.getName();
            
    // List of method matchers that declare method with signature patterns that we want to instrument
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<OpType>> opMethods = Arrays.asList(
        new MethodMatcher<OpType>("append", new String[]{ "org.apache.hadoop.hbase.client.OperationWithAttributes" }, "org.apache.hadoop.hbase.client.Result", OpType.SINGLE),
        new MethodMatcher<OpType>("batch", new String[]{ "java.util.List" }, "void", OpType.MULTI),
        new MethodMatcher<OpType>("batch", new String[]{ "java.util.List" }, "java.lang.Object", OpType.MULTI),
        
        new MethodMatcher<OpType>("checkAndDelete", new String[]{ "byte[]", "byte[]", "byte[]", "byte[]", "org.apache.hadoop.hbase.client.Delete" }, "boolean", OpType.CHECK),
        new MethodMatcher<OpType>("checkAndPut", new String[]{ "byte[]", "byte[]", "byte[]", "byte[]", "org.apache.hadoop.hbase.client.Put" }, "boolean", OpType.CHECK),
        
        new MethodMatcher<OpType>("delete", new String[]{ "org.apache.hadoop.hbase.client.OperationWithAttributes" }, "void", OpType.SINGLE),
        new MethodMatcher<OpType>("delete", new String[]{ "java.util.List" }, "void", OpType.MULTI),
        
        new MethodMatcher<OpType>("exists", new String[]{ "org.apache.hadoop.hbase.client.OperationWithAttributes" }, "boolean", OpType.SINGLE),
        new MethodMatcher<OpType>("exists", new String[]{ "java.util.List" }, "java.lang.Boolean[]", OpType.MULTI),
        
        new MethodMatcher<OpType>("get", new String[]{ "org.apache.hadoop.hbase.client.OperationWithAttributes" }, "org.apache.hadoop.hbase.client.Result", OpType.SINGLE),
        new MethodMatcher<OpType>("get", new String[]{ "java.util.List" }, "org.apache.hadoop.hbase.client.Result[]", OpType.MULTI),

        new MethodMatcher<OpType>("getRowOrBefore", new String[]{ "byte[]", "byte[]" }, "org.apache.hadoop.hbase.client.Result", OpType.GET_ROW_OR_BEFORE),
        
        new MethodMatcher<OpType>("mutateRow", new String[]{ "org.apache.hadoop.hbase.client.RowMutations" }, "void", OpType.SINGLE),
        
        new MethodMatcher<OpType>("increment", new String[]{ "org.apache.hadoop.hbase.client.Increment" }, "org.apache.hadoop.hbase.client.Result", OpType.SINGLE),
        new MethodMatcher<OpType>("incrementColumnValue", new String[]{ "byte[]", "byte[]", "byte[]" }, "long", OpType.INCREMENT_COLUMN),
        
        new MethodMatcher<OpType>("put", new String[]{ "org.apache.hadoop.hbase.client.OperationWithAttributes" }, "void", OpType.SINGLE),
        new MethodMatcher<OpType>("put", new String[]{ "java.util.List" }, "void", OpType.MULTI),
        
        new MethodMatcher<OpType>("coprocessorExec", new String[] {}, "java.util.Map", OpType.OTHER),
        new MethodMatcher<OpType>("coprocessorService", new String[] {}, "java.util.Map", OpType.OTHER),
        
        
        new MethodMatcher<OpType>("flushCommits", new String[]{ }, "void", OpType.OTHER)
        //new MethodMatcher<OpType>("setAutoFlush", new String[]{ "boolean" , "boolean" }, "void", OpType.OTHER)
   );
    
    @SuppressWarnings("unchecked")
    private static List<ConstructorMatcher<ConstructorType>> constructorMatchers = Arrays.asList(new ConstructorMatcher<ConstructorType>(new String[] { "org.apache.hadoop.conf.Configuration", "byte[]" }, ConstructorType.BYTE_ARRAY)
                                                                                               , new ConstructorMatcher<ConstructorType>(new String[] { "org.apache.hadoop.conf.Configuration", "org.apache.hadoop.hbase.TableName" }, ConstructorType.TABLE_NAME_2)
                                                                                               , new ConstructorMatcher<ConstructorType>(new String[] { "org.apache.hadoop.hbase.TableName" }, ConstructorType.TABLE_NAME_1));

    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
            throws Exception {
        try {
            CtClass hTableClass = classPool.get("org.apache.hadoop.hbase.client.HTable");
            
            //only instrument ctor if it HTable, do not instrument init for other classes such as table wrapper
            if (cc.equals(hTableClass)) {
                for (Entry<CtConstructor, ConstructorType> entry : findMatchingConstructors(cc, constructorMatchers).entrySet()) {
                    CtConstructor constructor = entry.getKey();
                    ConstructorType type = entry.getValue();
                    
                    String nameToken;
                    if (type == ConstructorType.BYTE_ARRAY) {
                        nameToken = "$2";
                    } else if (type == ConstructorType.TABLE_NAME_1) {
                        nameToken = "$1 != null ? $1.getName() : (byte[])null";
                    } else {
                        nameToken = "$2 != null ? $2.getName() : (byte[])null";
                    }
                    
                    insertBefore(constructor, CLASS_NAME + ".layerEntry(\"init\", " + nameToken + " , null);");
                    addErrorReporting(constructor, Throwable.class.getName(), LAYER_NAME, classPool);
                    insertAfter(constructor, CLASS_NAME + ".layerExit(\"init\", null);", true);
                }
                
                
            }
        } catch (NotFoundException e) {
            logger.warn("failed to locate constructor of HTable, cannot instrument table init");
        }
        
        
        Map<CtMethod, OpType> matchingMethods = findMatchingMethods(cc, opMethods);
        
        for (Entry<CtMethod, OpType> matchingMethodEntry : matchingMethods.entrySet()) {
            CtMethod method = matchingMethodEntry.getKey();
            
            if (shouldModify(cc, method)) {
            
                OpType opType = matchingMethodEntry.getValue();
                
                switch (opType) {
                case SINGLE:
                    insertBefore(method, CLASS_NAME + ".layerEntry(\"" + method.getName() + "\", getTableName(), $1);");
                    break;
                case MULTI:
                    insertBefore(method, CLASS_NAME + ".layerEntry(\"" + method.getName() + "\", getTableName(), $1.size());");
                    break;
                case CHECK:
                    insertBefore(method, CLASS_NAME + ".layerEntry(\"" + method.getName() + "\", getTableName(), $5);");
                    break;
//                case ROW:
//                    insertBefore(method, CLASS_NAME + ".layerEntry(\"" + method.getName() + "\", getTableName(), $1.getRow());");
//                    break;
                case INCREMENT_COLUMN:
                    insertBefore(method, 
                                 "java.util.Map extraValues = new java.util.HashMap();" +
                                 "if ($1 != null) { extraValues.put(\"Row\", $1); }" +
                                 "if ($2 != null) { " +
                                 "    extraValues.put(\"CF\", $2); " +
                                 "    if ($3 != null) { " +
                                 "        extraValues.put(\"Qualifier\", $3); " +
                                 "    } " +
                                 "}" +
                         		 CLASS_NAME + ".layerEntry(\"" + method.getName() + "\", getTableName(), extraValues);");
                    break;
                case GET_ROW_OR_BEFORE:
                    insertBefore(method, 
                                 "java.util.Map extraValues = new java.util.HashMap();" +
                                 "if ($1 != null) { extraValues.put(\"Row\", $1); }" +
                                 "if ($2 != null) { extraValues.put(\"CF\", $2); }" +
                                 CLASS_NAME + ".layerEntry(\"" + method.getName() + "\", getTableName(), extraValues);");
                    break;
                case OTHER:
                    insertBefore(method, CLASS_NAME + ".layerEntry(\"" + method.getName() + "\", getTableName(), null);");
                    break;
                default:
                    logger.warn("OpType not handled [" + opType + "] of method [" + method.getName() + "]");
                    insertBefore(method, CLASS_NAME + ".layerEntry(\"" + method.getName() + "\", getTableName(), null);");
                }
                
                if (method.getReturnType().subtypeOf(classPool.get(Map.class.getName()))) {
                    insertAfter(method, CLASS_NAME + ".layerExit(\"" + method.getName() + "\", $_.size());", true);
                } else {
                    insertAfter(method, CLASS_NAME + ".layerExit(\"" + method.getName() + "\", $_);", true);
                }
                    		
            }
        }
        
        return true;
    }

    
    public static void layerEntry(String opName, byte[] tableName, Object operation) {
        layerEntry(opName, tableName, extractOperationInfo(operation));
    }
    
    public static void layerEntry(String opName, byte[] tableName, int opSize) {
        layerEntry(opName, tableName, Collections.singletonMap("OpSize", opSize));
    }
    
    public static void layerEntry(String opName, byte[] tableName, byte[] row) {
        layerEntry(opName, tableName, row != null ? Collections.singletonMap("Row", row) : null);
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
    

    public static void layerExit(String opName, Object returnValue) {
        layerExit(opName, null);
    }
    
    public static void layerExit(String opName, boolean returnValue) {
        layerExit(opName, Collections.singletonMap("ResultValue", returnValue));
    }
    
    public static void layerExit(String opName, long returnValue) {
        layerExit(opName, Collections.singletonMap("ResultValue", returnValue));
    }
    
    public static void layerExit(String opName, int returnValueSize) {
        layerExit(opName, Collections.singletonMap("ResultCount", returnValueSize));
    }
    
    private static void layerExit(String opName, Map<String, Object> extraValues) {
        if (shouldEndExtent()) {
            Event event = Context.createEvent();
            event.addInfo("Layer", LAYER_NAME,
                          "Label", "exit",
                          "Query", opName);
            
            if (extraValues != null) {
                event.addInfo(extraValues);
            }
            
            event.report();
        }
    }
    
    
    
    private enum OpType {
        SINGLE, MULTI, CHECK, INCREMENT_COLUMN, GET_ROW_OR_BEFORE, OTHER
    }
    
    private enum ConstructorType {
        BYTE_ARRAY, TABLE_NAME_1, TABLE_NAME_2
    }
}