package com.tracelytics.instrumentation.http;

import java.net.URL;

public interface HttpURLConnection {
    boolean isTvInputStreamReported();
    void setTvInputStreamReported(boolean inputStreamReported);
    
    //To handle redirect that might switch the Http method during execution
    String tvGetEntryHttpMethod();
    void tvSetEntryHttpMethod(String httpMethod);
    
    //For redirect detection 
    URL tvGetEntryUrl();
    void tvSetEntryUrl(URL entryUrl);
    
    String tvGetEntryXTraceId();
    void tvSetEntryXTraceId(String entryXTraceId);
}
