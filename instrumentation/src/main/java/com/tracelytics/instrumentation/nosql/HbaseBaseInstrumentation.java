package com.tracelytics.instrumentation.nosql;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.TvContextObjectAware;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.Metadata;

/**
 * Abstract base class for HBase instrumentation
 * @author Patson Luk
 *
 */
public abstract class HbaseBaseInstrumentation extends ClassInstrumentation {
    protected static final String LAYER_NAME = "hbase";
    protected static final String FLAVOR = "hbase";
    
    private static ThreadLocal<Integer> depthThreadLocal = new ThreadLocal<Integer>() {
        protected Integer initialValue() {
            return 0;
        }
    };
    
    //keep track of currently instrumenting scanner operation
    private static ThreadLocal<String> currentScannerOperationThreadLocal = new ThreadLocal<String>();

    /**
     * Checks whether the current instrumentation should start a new extent. If there is already an active Hbase extent, then do not start one
     * @return
     */
    protected static boolean shouldStartExtent() {
        int currentDepth = depthThreadLocal.get();
        depthThreadLocal.set(currentDepth + 1);

        if (currentDepth == 0) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Checks whether the current instrumentation should end the current extent. If this is the active Hbase extent being traced, then ends it
     * @return
     */
    protected static boolean shouldEndExtent() {
        int currentDepth = depthThreadLocal.get();
        depthThreadLocal.set(currentDepth - 1);

        if (currentDepth == 1) {
            return true;
        } else {
            return false;
        }
    }
    
    /**
     * Returns whether there are any active Hbase extent
     * @return
     */
    static boolean hasActiveExtent() {
        return depthThreadLocal.get() != null && depthThreadLocal.get() > 0;
    }
    
    public static Map<String, Object> extractOperationInfo(Object operation) {
        Map<String, Object> operationInfo = new HashMap<String, Object>();
        
        if (operation instanceof HbaseOperationWithFamilyInfo) {
            Map<byte[], ?> families = ((HbaseOperationWithFamilyInfo)operation).getTvFamilies();
            
            List<String> familyInfo = new ArrayList<String>(); 
            for (Entry<byte[], ?> familyEntry : families.entrySet()) {
                String family = toStringBinary(familyEntry.getKey());
                
                if (familyEntry.getValue() == null) { //all columns in the family
                    familyInfo.add(family);
                } else { //column with qualifiers, iterate through the entry and produce qualifier in format of [column family]:[qualifier]
                    Collection<?> qualifiers;
                    
                    if (familyEntry.getValue() instanceof Collection) {
                        qualifiers = (Collection<?>) familyEntry.getValue();
                    } else if (familyEntry.getValue() instanceof Map) {
                        qualifiers = ((Map<?, ?>)familyEntry.getValue()).keySet();
                    } else {
                        logger.warn("Unexpected family entry object in getTvFamilies, found [" + familyEntry != null ? familyEntry.getClass().getName() : "null" + "]");
                        continue;
                    }
                    
                    for (Object qualifier : qualifiers) {
                        if (qualifier instanceof HbaseObjectWithQualifier) {
                            familyInfo.add(family + ":" + toStringBinary(((HbaseObjectWithQualifier)qualifier).getQualifier()));
                        } else if (qualifier instanceof byte[]) {
                            familyInfo.add(family + ":" + toStringBinary((byte[]) qualifier));
                        } else {
                            logger.warn("Unable to capture qualifier, unexpected class [" + qualifier.getClass().getName() + "]");
                        }
                    }
                }
            }
            
            if (!familyInfo.isEmpty()) {
                if (familyInfo.size() == 1) {
                    operationInfo.put("CF", familyInfo.iterator().next());
                } else {
                    operationInfo.put("CF", familyInfo.toArray(new String[0]));
                }
            }
        }
        
                  
        if (operation instanceof HbaseOperationWithRow) {
            byte[] row = ((HbaseOperationWithRow)operation).getTvRowKey();
            if (row != null) {
                operationInfo.put("Row", row);
            }
        }
        
        return operationInfo;
    }
    
    protected static String encodeTableName(byte[] tableName) throws UnsupportedEncodingException {
        if (tableName != null) {
            String encodedTableName = toStringBinary(tableName);
            return encodedTableName.replace(".", "\\.");
        } else {
            return null;
        }
    }
    
    /**
     * Only print a set of ascii characters. Otherwise escape with \x
     * 
     * Referencing org.apache.hadoop.hbase.util.Bytes.toStringBinary(byte[], int, int)
     */
    protected static String toStringBinary(byte[] b) {
        if (b == null) {
            return null;
        }
        
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < b.length ; ++i ) {
          int ch = b[i] & 0xFF;
          if ( (ch >= '0' && ch <= '9')
              || (ch >= 'A' && ch <= 'Z')
              || (ch >= 'a' && ch <= 'z')
              || " `~!@#$%^&*()-_=+[]{}|;:'\",.<>/?".indexOf(ch) >= 0 ) {
            result.append((char)ch);
          } else {
            result.append(String.format("\\x%02X", ch));
          }
        }
        return result.toString();
    }
    
    /**
     * Sets the currently instrumented scanner operation on this thread 
     * @param scannerOperation
     */
    protected static void setCurrentInstrumentedScannerOperation(String scannerOperation) {
        currentScannerOperationThreadLocal.set(scannerOperation);
    }
    
    /**
     * Gets the currently instrumented scanner operation on this thread, null if no scanner operation is currently instrumented
     * @return
     */
    protected static String getCurrentInstrumentedScannerOperation() {
        return currentScannerOperationThreadLocal.get();
    }
    
    public static void tagContext(Object runnableObject) {
        TvContextObjectAware runnable = (TvContextObjectAware) runnableObject;
        runnable.setTvContext(Context.getMetadata());
        runnable.setTvFromThreadId(Thread.currentThread().getId());
    }
    
    public static void setContext(Object runnableObject) {
        TvContextObjectAware runnable = (TvContextObjectAware) runnableObject;
        if (runnable.getTvFromThreadId() != Thread.currentThread().getId()) {
            Metadata context= ((TvContextObjectAware) runnableObject).getTvContext();
            if (context != null) {
                Metadata forkedContext = new Metadata(context);//use a clone to create a fork
                forkedContext.setIsAsync(true);
                Context.setMetadata(forkedContext); 
            }
        }
    }
    
    public static void clearContext(Object runnableObject) {
        TvContextObjectAware runnable = (TvContextObjectAware) runnableObject;
        if (runnable.getTvFromThreadId() != Thread.currentThread().getId()) {
            Context.clearMetadata();
        }
    }
    
}
