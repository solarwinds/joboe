package com.tracelytics.instrumentation.http;

import java.net.URI;

public interface HttpResponse {
    int statusCode();
    String tvGetHeader(String headerName);
}
