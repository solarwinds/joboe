package com.tracelytics.instrumentation.jdbc;

import java.util.SortedMap;

/**
 * Data associated with java.sql.PreparedStatement.
 * 
 * We use this to track the setXXX (setInt, setBooleam, setString etc) and setObject methods in order to provide parameter values in our traces
 */
public interface PreparedStatement {
    // our added methods:
    void tlysSetParameter(int index, Object parameter);
    SortedMap<Integer, Object> tlysGetParameters();
}