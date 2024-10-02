package com.tracelytics.instrumentation.http.netty;

import java.util.Iterator;
import java.util.Map.Entry;

/**
 * Tags the <code>io.netty.handler.codec.http2.Http2Headers</code> as valid candidate as both request and response
 * @author pluk
 *
 */
public interface NettyHttp2Headers extends NettyHttpRequest, NettyHttpResponse{
    Iterator<Entry<CharSequence, CharSequence>> iterator();
}
