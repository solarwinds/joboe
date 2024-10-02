package com.tracelytics.instrumentation.http.spray;

import com.tracelytics.instrumentation.TvContextObjectAware;
import com.tracelytics.joboe.span.impl.Span;

/**
 * Tags the <code>spray.http.HttpRequest</code> to provide:
 * <ol>
 *  <li>Convenient method to clone a new HttpRequest with a header value (replace if already exists)</li>
 *  <li>Flags on whether certain events have been reported (to avoid duplicated instrumentation)</li>
 * <ol> 
 * @author pluk
 *
 */
public interface SprayHttpRequest extends TvContextObjectAware {
    /**
     * convenient method to clone a HttpRequest, if the a header with this name is already exist, it will be replaced with the new value
     * @param headerName
     * @param headerValue
     * @return
     */
    public Object tvWithHeader(String headerName, String headerValue);

    public void setTvSprayCanExitReported(boolean sprayCanExitReported);
    public boolean getTvSprayCanExitReported();
    
    public void setTvSprayRoutingSpan(Span span);
    public Span getTvSprayRoutingSpan();
    
    void tvSetSslEncryption(boolean sslEncryption);
    boolean tvGetSslEncryption();
    
    public String tvUriPath();
    public String tvUriQuery();
    public String tvMethod();
}
