package com.tracelytics.instrumentation.http.akka.server;

public interface AkkaHttpResponse {

    int tvStatusCode();

    /**
     * Take note that Akka HttpResponse is Immutable, therefore this method will create a clone instance with the new header appended
     * @param key
     * @param value
     */
    AkkaHttpResponse tvAddHeader(String key, String value);

    String tvGetHeader(String key);
}