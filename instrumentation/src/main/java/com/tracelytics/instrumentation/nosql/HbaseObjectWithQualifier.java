package com.tracelytics.instrumentation.nosql;

/**
 * Represents Hbase class that provides Qualifier inforatiom
 * @author Patson Luk
 *
 */
public interface HbaseObjectWithQualifier {
    byte[] getQualifier();
}
