package com.appoptics.instrumentation.nosql.mongo3;

import com.tracelytics.instrumentation.TvContextObjectAware;


public interface Mongo3Callback {
    String tvGetRemoteHost();
    void tvSetRemoteHost(String remoteHost);
}