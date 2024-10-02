package com.tracelytics.instrumentation.nosql;

import java.util.Collection;
import java.util.Map;

/**
 * Represents Hbase class that provides Column Family Information
 * 
 * @author Patson Luk
 *
 */
public interface HbaseOperationWithFamilyInfo {
    Map<byte[], ? extends Collection<byte[]>> getTvFamilies();
}
