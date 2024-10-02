package com.tracelytics.instrumentation.http.undertow;

import com.tracelytics.instrumentation.SpanAware;

public interface UndertowHttpServerExchange extends SpanAware {
    String tvGetRequestHeader(String headerKey);
    String tvGetRemoteClient();
    String tvGetRequestMethod();

    /**
     * Overwrites and update any header with the specified headerKey and headerValue
     * @param headerKey
     * @param headerValue
     */
    void tvSetRequestHeader(String headerKey, String headerValue);
    void tvAddResponseHeader(String headerKey, String headerValue);
    String tvGetResponseHeader(String headerKey);
    void tvSetExitXTrace(String exitXTrace);
    String tvGetExitXTrace();
    boolean tvGetHasChecked();
    void tvSetHasChecked(boolean hasChecked);
    
    String getRequestURI();
    int getResponseCode();
    String getHostAndPort();
    String getRequestScheme();
    String getQueryString();
}
