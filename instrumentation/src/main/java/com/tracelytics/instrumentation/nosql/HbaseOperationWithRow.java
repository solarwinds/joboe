package com.tracelytics.instrumentation.nosql;

/**
 * Represents Hbase class that provides Row Information
 * 
 * @author Patson Luk
 *
 */
public interface HbaseOperationWithRow {
    byte[] getTvRowKey();
}
