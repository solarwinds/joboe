package com.tracelytics.instrumentation.cache.ehcache;

/**
 * Tags net.sf.ehcache.search.Results and exposes its <code>size()</code> method
 * @author pluk
 *
 */
public interface EhcacheSearchResults {
    int size();
}
