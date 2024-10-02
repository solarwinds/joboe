package com.tracelytics.instrumentation.nosql;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtConstructor;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.instrumentation.ConstructorMatcher;
import com.tracelytics.instrumentation.MethodMatcher;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.Event;

/**
 * Instrumentation for the Hbase Admin interface. The interface is mainly used for table and column family operations
 * @author Patson Luk
 *
 */
public class HbaseAdminInstrumentation extends HbaseBaseInstrumentation {
    private static final String CLASS_NAME = HbaseAdminInstrumentation.class.getName();
   
    private static final MethodType CREATE_TABLE = new MethodType(OpType.CREATE_TABLE, false);
    private static final MethodType GENERAL_TABLE_BY_BYTE_ARRAY = new MethodType(OpType.GENERAL_TABLE, true);
    private static final MethodType GENERAL_TABLE_BY_TABLE_NAME = new MethodType(OpType.GENERAL_TABLE, false);
    private static final MethodType GENERAL_COLUMN_BY_BYTE_ARRAY = new MethodType(OpType.GENERAL_COLUMN, true);
    private static final MethodType GENERAL_COLUMN_BY_TABLE_NAME = new MethodType(OpType.GENERAL_COLUMN, false);
    private static final MethodType DELETE_COLUMN_BY_BYTE_ARRAY = new MethodType(OpType.DELETE_COLUMN, true);
    private static final MethodType DELETE_COLUMN_BY_TABLE_NAME = new MethodType(OpType.DELETE_COLUMN, false);

    // List of method matchers that declare method with signature patterns that we want to instrument
    @SuppressWarnings("unchecked")
    private static List<MethodMatcher<MethodType>> opMethods = Arrays.asList(
    // table operations                                                                         
        new MethodMatcher<MethodType>("createTable", new String[]{ "org.apache.hadoop.hbase.HTableDescriptor" }, "void", CREATE_TABLE),
        new MethodMatcher<MethodType>("createTableAsync", new String[]{ "org.apache.hadoop.hbase.HTableDescriptor" }, "void", CREATE_TABLE),
        new MethodMatcher<MethodType>("modifyTable", new String[]{ "byte[]" }, "void", GENERAL_TABLE_BY_BYTE_ARRAY),
        new MethodMatcher<MethodType>("modifyTable", new String[]{ "org.apache.hadoop.hbase.TableName" }, "void", GENERAL_TABLE_BY_TABLE_NAME),
        new MethodMatcher<MethodType>("deleteTable", new String[]{ "byte[]" }, "void", GENERAL_TABLE_BY_BYTE_ARRAY),
        new MethodMatcher<MethodType>("deleteTable", new String[]{ "org.apache.hadoop.hbase.TableName" }, "void", GENERAL_TABLE_BY_TABLE_NAME),
        
        new MethodMatcher<MethodType>("enableTable", new String[]{ "byte[]" }, "void", GENERAL_TABLE_BY_BYTE_ARRAY),
        new MethodMatcher<MethodType>("enableTable", new String[]{ "org.apache.hadoop.hbase.TableName" }, "void", GENERAL_TABLE_BY_TABLE_NAME),
        new MethodMatcher<MethodType>("enableTableAsync", new String[]{ "byte[]" }, "void", GENERAL_TABLE_BY_BYTE_ARRAY),
        new MethodMatcher<MethodType>("enableTableAsync", new String[]{ "org.apache.hadoop.hbase.TableName" }, "void", GENERAL_TABLE_BY_TABLE_NAME),
        new MethodMatcher<MethodType>("disableTable", new String[]{ "byte[]" }, "void", GENERAL_TABLE_BY_BYTE_ARRAY),
        new MethodMatcher<MethodType>("disableTable", new String[]{ "org.apache.hadoop.hbase.TableName" }, "void", GENERAL_TABLE_BY_TABLE_NAME),
        new MethodMatcher<MethodType>("disableTableAsync", new String[]{ "byte[]" }, "void", GENERAL_TABLE_BY_BYTE_ARRAY),
        new MethodMatcher<MethodType>("disableTableAsync", new String[]{ "org.apache.hadoop.hbase.TableName" }, "void", GENERAL_TABLE_BY_TABLE_NAME),
        
        new MethodMatcher<MethodType>("isTableAvailable", new String[]{ "byte[]" }, "boolean", GENERAL_TABLE_BY_BYTE_ARRAY),
        new MethodMatcher<MethodType>("isTableAvailable", new String[]{ "org.apache.hadoop.hbase.TableName" }, "boolean", GENERAL_TABLE_BY_TABLE_NAME),
        new MethodMatcher<MethodType>("isTableDisabled", new String[]{ "byte[]" }, "boolean", GENERAL_TABLE_BY_BYTE_ARRAY),
        new MethodMatcher<MethodType>("isTableDisabled", new String[]{ "org.apache.hadoop.hbase.TableName" }, "boolean", GENERAL_TABLE_BY_TABLE_NAME),
        new MethodMatcher<MethodType>("isTableEnabled", new String[]{ "byte[]" }, "boolean", GENERAL_TABLE_BY_BYTE_ARRAY),
        new MethodMatcher<MethodType>("isTableEnabled", new String[]{ "org.apache.hadoop.hbase.TableName" }, "boolean", GENERAL_TABLE_BY_TABLE_NAME),
        
    // column operations
        new MethodMatcher<MethodType>("addColumn", new String[]{ "byte[]", "org.apache.hadoop.hbase.HColumnDescriptor" }, "void", GENERAL_COLUMN_BY_BYTE_ARRAY),
        new MethodMatcher<MethodType>("addColumn", new String[]{ "org.apache.hadoop.hbase.TableName", "org.apache.hadoop.hbase.HColumnDescriptor" }, "void", GENERAL_COLUMN_BY_TABLE_NAME),
        new MethodMatcher<MethodType>("modifyColumn", new String[]{ "byte[]", "org.apache.hadoop.hbase.HColumnDescriptor" }, "void", GENERAL_COLUMN_BY_BYTE_ARRAY),
        new MethodMatcher<MethodType>("modifyColumn", new String[]{ "org.apache.hadoop.hbase.TableName", "org.apache.hadoop.hbase.HColumnDescriptor" }, "void", GENERAL_COLUMN_BY_TABLE_NAME),
        new MethodMatcher<MethodType>("deleteColumn", new String[]{ "byte[]", "byte[]" }, "void", DELETE_COLUMN_BY_BYTE_ARRAY),
        new MethodMatcher<MethodType>("deleteColumn", new String[]{ "org.apache.hadoop.hbase.TableName", "byte[]" }, "void", DELETE_COLUMN_BY_TABLE_NAME)
        
   );
    
    @SuppressWarnings("unchecked")
    private static List<ConstructorMatcher<Object>> constructorMatchers = Arrays.asList(new ConstructorMatcher<Object>(new String[0])); //do not care about param type
                                                                                               
    

    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
            throws Exception {
        for (CtConstructor constructor : findMatchingConstructors(cc, constructorMatchers).keySet()) {
            insertBefore(constructor, CLASS_NAME + ".layerTableEntry(\"init_admin\", null);");
            addErrorReporting(constructor, Throwable.class.getName(), LAYER_NAME, classPool);
            insertAfter(constructor, CLASS_NAME + ".layerExit(\"init_admin\");", true);
        }
        
        Map<CtMethod, MethodType> matchingMethods = findMatchingMethods(cc, opMethods);
        
        for (Entry<CtMethod, MethodType> matchingMethodEntry : matchingMethods.entrySet()) {
            CtMethod method = matchingMethodEntry.getKey();
            
            if (shouldModify(cc, method)) {
            
                MethodType methodType = matchingMethodEntry.getValue();
                OpType opType = methodType.type;
                
                String tableNameToken = methodType.isByByteArray ? "$1" : "$1 != null ? $1.getName() : (byte[]) null";
                
                switch (opType) {
                case GENERAL_TABLE:
                    insertBefore(method, CLASS_NAME + ".layerTableEntry(\"" + method.getName() + "\", " + tableNameToken + ");");
                    insertAfter(method, CLASS_NAME + ".layerExit(\"" + method.getName() + "\");", true);
                    break;
                case CREATE_TABLE:
                    insertBefore(method, CLASS_NAME + ".layerTableEntry(\"" + method.getName() + "\", $1 != null ? $1.getName() : (byte[]) null);");
                    insertAfter(method, CLASS_NAME + ".layerExit(\"" + method.getName() + "\");", true);
                    break;
                case GENERAL_COLUMN:
                    insertBefore(method, CLASS_NAME + ".layerColumnEntry(\"" + method.getName() + "\", " + tableNameToken + ", $2 != null ? $2.getName() : (byte[]) null);");
                    insertAfter(method, CLASS_NAME + ".layerExit(\"" + method.getName() + "\");", true);
                    break;
                case DELETE_COLUMN:
                    insertBefore(method, CLASS_NAME + ".layerColumnEntry(\"" + method.getName() + "\", " + tableNameToken + ", $2);");
                    insertAfter(method, CLASS_NAME + ".layerExit(\"" + method.getName() + "\");", true);
                    break;
                default:
                    logger.warn("OpType not handled [" + opType + "] of method [" + method.getName() + "]");
                }
            }
        }
        
        return true;
    }

    
    public static void layerTableEntry(String opName, byte[] tableName) {
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
            
            event.report();
        }
        
        
    }
    
    public static void layerColumnEntry(String opName, byte[] tableName, byte[] columnName) {
        if (shouldStartExtent()) {
            Event event = Context.createEvent();
            event.addInfo("Layer", LAYER_NAME,
                          "Label", "entry",
                          "Flavor", FLAVOR,
                          "Query", opName);
            
            if (tableName != null) {
                try {
                    event.addInfo("Table", encodeTableName(tableName));
                } catch (UnsupportedEncodingException e) {
                    logger.warn("Cannot encode the HBase table name, " + e.getMessage());
                }
            }
            
            if (columnName != null) {
                event.addInfo("CF", toStringBinary(columnName));
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
    
    
    
    private enum OpType {
        GENERAL_TABLE, 
        CREATE_TABLE,
        GENERAL_COLUMN, 
        DELETE_COLUMN 
    }
    
    private static class MethodType {
        private OpType type;
        private boolean isByByteArray;
        
        private MethodType(OpType type, boolean isByByteArray) {
            this.type = type;
            this.isByByteArray = isByByteArray;
        }
    }
}