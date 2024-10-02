package com.tracelytics.instrumentation.http.spray;


/**
 * Tags the <code>spray.http.HttpResponse</code> to provide:
 * <ol>
 *  <li>Convenient method to get status code and the header of the response</li>
 * <ol> 
 * @author pluk
 *
 */
public interface SprayHttpResponse {
    int tvGetStatusCode();
    String tvGetResponseHeader(String key);
}
