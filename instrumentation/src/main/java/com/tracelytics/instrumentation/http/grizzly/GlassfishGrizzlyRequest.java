package com.tracelytics.instrumentation.http.grizzly;

public interface GlassfishGrizzlyRequest {
    String getHeader(String headerKey);

    String getRequestURI();

    String getQueryString();

    String tvGetMethod();

    String getServerName();

    String getScheme();

    String getRemoteAddr();

    void tvSetRequestHeader(String key, String value);
}
