package com.tracelytics.instrumentation.http;

import com.tracelytics.instrumentation.TvContextObjectAware;

import java.net.URI;
import java.util.List;
import java.util.Map;

public interface HttpRequest extends TvContextObjectAware {
    String method();
    URI uri();
    Map<String, List<String>> tvHeadersAsMap();
}
