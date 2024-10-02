package com.tracelytics.instrumentation.nosql.cassandra;

/**
 * Cassandra Session with keyspace information
 * @author pluk
 *
 */
public interface SessionWithKeyspace {
    public String tvGetKeyspace();
    public void tvSetKeyspace(String keyspace);
}
