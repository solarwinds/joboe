package com.tracelytics.instrumentation.cache.ehcache;

/**
 * Tags net.sf.ehcache.Element and exposes its methods
 * @author pluk
 *
 */
public interface EhcacheElement {
    Object getObjectKey();
    long getCreationTime();
    long getHitCount();
    long getLastAccessTime();
    long getLastUpdateTime();
    long getVersion();
}
