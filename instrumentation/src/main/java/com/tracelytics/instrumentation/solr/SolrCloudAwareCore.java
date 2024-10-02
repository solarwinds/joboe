package com.tracelytics.instrumentation.solr;

public interface SolrCloudAwareCore {
    public String tvGetShardId(); //added method
    public String tvGetCollectionName(); //added method
}
