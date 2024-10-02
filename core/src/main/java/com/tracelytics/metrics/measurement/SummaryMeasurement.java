package com.tracelytics.metrics.measurement;

/**
 * A {@link Measurement} that holds the summary of a series of values provided by {@link recordValue} calls. 
 * 
 * Currently this only keep track of count and sum of the value series.
 * 
 * @author pluk
 *
 */
public abstract class SummaryMeasurement<T extends Number> extends Measurement<T> {
    private long count;
    
    //might implement full summary at https://www.librato.com/docs/api/#create-a-measurement in future
    
    @Override
    public synchronized void recordValue(T value) {
        updateSum(value);
        incrementCount(1);
    }
    
    protected abstract void updateSum(T value);
    public abstract T getSum();
    
    public void incrementCount(int increment) {
        this.count += increment;
    }
    
    public long getCount() {
        return count;
    }
}
