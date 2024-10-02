package com.tracelytics.instrumentation;

/**
 * Builder that builds an Instrumentation
 * @author pluk
 *
 * @param <T>
 */
public interface InstrumentationBuilder<T> {
    T build() throws Exception;
}
