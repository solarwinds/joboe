package com.tracelytics.instrumentation.cache.spymemcached;

import java.util.Map;

import com.tracelytics.instrumentation.TvContextObjectAware;
import com.tracelytics.joboe.Metadata;

public interface SpyMemcachedCallback extends TvContextObjectAware {
    String getTvStatusString();
    void setTvStatusString(String statusString);
    Map<Metadata, Integer> getTvValueLengths();

}
