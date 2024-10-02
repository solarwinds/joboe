package com.tracelytics.instrumentation.jdbc;


/**
 * Data associated with java.sql.Statement
 */
public interface Statement {
    // our added methods:
    void tlysSetSQL(String sql);
    String tlysGetSQL() throws Exception;
    void tlysSetDB(String sql);
    String tlysGetDB() throws Exception;
    int tlysIncBatchSize();
    void tlysClearBatchSize();
    int tlysGetBatchSize();
    void tlysSetHost(String host);
    String tlysGetHost() throws Exception;
    boolean tlysIsHostSet();
}