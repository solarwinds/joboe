package com.tracelytics.instrumentation.http.spray;

import java.net.InetSocketAddress;

import com.tracelytics.instrumentation.TvContextObjectAware;


/**
 * Tags the <code>spray.http.HttpResponse</code> to provide:
 * <ol>
 *  <li>Convenient method to get status code and the header of the response</li>
 * <ol> 
 * @author pluk
 *
 */
public interface SprayHttpClientConnection {
    boolean tvGetSslEncryption();
    InetSocketAddress tvGetRemoteAddress();
}
