package com.tracelytics.instrumentation.nosql.cassandra;

import java.util.SortedMap;

/**
 * Cassandra Statement with parameters
 * @author pluk
 *
 */
public interface StatementWithParameters {
    // our added methods:
    void tvSetParameter(int index, Object parameter);
    SortedMap<Integer, Object> tvGetParameters();
}
