package com.tracelytics.instrumentation.nosql;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.List;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtMethod;
import com.tracelytics.ext.javassist.NotFoundException;
import com.tracelytics.instrumentation.MethodMatcher;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.Event;
import com.tracelytics.joboe.config.ConfigManager;
import com.tracelytics.joboe.config.ConfigProperty;

/**
 * Instruments Scanner operation. Take note that scanner works quite differently from other operations which is applied directly to class HTable
 * 
 * Scanner are created by Htable.getScanner() method and then later on used to iterate through a range of rows. Network roundtrip is not made
 * until the methods of the Scanner are invoked
 * 
 * 
 * 
 * @author Patson Luk
 *
 */
public class HbaseScannerCallableInstrumentation extends HbaseBaseInstrumentation {
    private static final String CLASS_NAME = HbaseScannerCallableInstrumentation.class.getName();
    
    //whether to instrument scanner "next" operations
    private static boolean instrumentScannerFlag = ConfigManager.getConfig(ConfigProperty.AGENT_HBASE_SCANNER_NEXT) != null ? (Boolean) ConfigManager.getConfig(ConfigProperty.AGENT_HBASE_SCANNER_NEXT) : false;

    private static final List<MethodMatcher<Object>> methodMatchers = Arrays.asList(new MethodMatcher<Object>("call", new String[] {}, "org.apache.hadoop.hbase.client.Result[]"));
    
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
            throws Exception {
        boolean supportScannerId = false;
        boolean supportClosed = false;
        String tableNameFragment = null;
        try {
            cc.getField("scannerId", "J");
            supportScannerId = true;
        } catch (NotFoundException e) {
            logger.warn("Cannot report Scanner Id there is no [scannerId] field available");
        }
        
        try {
            cc.getField("closed", "Z");
            supportClosed = true;
        } catch (NotFoundException e) {
            logger.warn("Cannot report closed there is no [closed] field available");
        }
        
        try {
            cc.getField("tableName", "[B");
            tableNameFragment = "tableName"; 
        } catch (NotFoundException e) {
            //try the getTableName method which is available in 0.96+
            try {
                cc.getMethod("getTableName", "()Lorg/apache/hadoop/hbase/TableName;");
                tableNameFragment = "getTableName().getName()";
            } catch (NotFoundException e1) {
                logger.warn("Cannot report closed there is no [closed] field available");
            }
        }
            
        for (CtMethod callMethod : findMatchingMethods(cc, methodMatchers).keySet()) {
            insertBefore(callMethod, CLASS_NAME + ".layerEntry(" +
            		tableNameFragment + ", " +
            		(supportScannerId ? "new Long(scannerId), " : "null, ") +
            		(supportClosed ? "closed, " : "false, ") +
            		"getCaching(), getScan());");
            insertAfter(callMethod, CLASS_NAME + ".layerExit(" +
                    (supportScannerId ? "new Long(scannerId), " : "null, ") + 
                    "$_ != null ? new Integer($_.length) : null);", true);
        }

        return true;
    }
    
    
    
    
    public static void layerEntry(byte[] tableName, Long scannerId, boolean closed, int caching, Object scanObject) {
        if (shouldStartExtent()) {
            //identify the query
            String query = getQuery(scannerId, closed);
            
            if ("scanner_next".equals(query) && !instrumentScannerFlag) { //check the flag AGENT_HBASE_SCANNER, do not proceed if it's not enabled
                return ;
            }
            
            //store the query that are instrumenting 
            setCurrentInstrumentedScannerOperation(query);

            
            Event event = Context.createEvent();
            event.addInfo("Layer", LAYER_NAME,
                          "Flavor", FLAVOR,
                          "Label", "entry",
                          "Query", query);
            
            if (tableName != null) {
                try {
                event.addInfo("Table", encodeTableName(tableName));
                } catch (UnsupportedEncodingException e) {
                    logger.warn("Cannot encode the HBase table name, " + e.getMessage());
                }
            }
                          
                        
            if (scannerId != null && scannerId != -1L) {
                event.addInfo("ScannerId", scannerId);
            }
        
            event.addInfo("Caching", caching);
            event.addInfo(extractOperationInfo(scanObject));
            event.report();
        }
    }
    
    /**
     * Identifies query type based on scannerId and the closed flag in ScannerCallable
     * @param scannerId
     * @param closed
     * @return
     */
    private static String getQuery(Long scannerId, boolean closed) {
        if (scannerId != null) {
            if (closed) {
                return "scanner_close";
            } else {
                if (scannerId == -1L) {
                    return "scanner_open"; //opening scanner, so id is not assigned yet
                } else {
                    return "scanner_next"; //already have an id, so it's the "next" operation
                }
            }
        } else {
            return "scanner";
        }
    }




    public static void layerExit(Long scannerId, Integer resultSize) {
        if (shouldEndExtent()) {
            String query = getCurrentInstrumentedScannerOperation();
            if (query == null) {
                return; //do not create exit event as there's no instrumented scanner opertion (no active entry)
            } else {
                setCurrentInstrumentedScannerOperation(null); //clear it
            }
                        
            Event event = Context.createEvent();
            event.addInfo("Layer", LAYER_NAME,
                          "Label", "exit");
            
            if (scannerId != null && scannerId != -1L) { //if scannerId is null then we do not have full info
                event.addInfo("ScannerId", scannerId);
            }
            
            if (resultSize != null) { //next operation, report result size as well
                event.addInfo("ResultCount", resultSize);
            }
            
            event.report();
        }
    }
}