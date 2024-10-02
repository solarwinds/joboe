package com.tracelytics.instrumentation.http.jetty;

import java.net.URI;

public interface JettyHttpRequest {
    String tvGetMethod();
    URI getURI();
    void tvHeader(String key, String value);
}
