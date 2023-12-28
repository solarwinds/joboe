package com.solarwinds.joboe.core.metrics.measurement;

/**
 * A metrics measurement which the state might be mutated by {@link recordValue} call based on the concrete implementation
 * @author pluk
 *
 */
public abstract class Measurement<T extends Number> {
    abstract void recordValue(T value);
}
