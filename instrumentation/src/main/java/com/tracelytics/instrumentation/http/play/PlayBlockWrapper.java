package com.tracelytics.instrumentation.http.play;

/**
 * Tags as a wrapper that wraps other blocks (scala.Function)
 * @author pluk
 *
 */
public interface PlayBlockWrapper {
    /**
     * Recursively looks up the wrapped block
     * @return
     */
    Object getWrappedBlock();
}
