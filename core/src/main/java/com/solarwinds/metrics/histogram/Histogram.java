package com.solarwinds.metrics.histogram;

/**
 * A common interface of the Histogram to be used for metrics aggregation. This does not tie to any specific histogram implementation.
 * 
 * For any histogram implementation used, an adaptor is expected to be written to bridge the concrete implementation with this interface
 * 
 * @see    HdrHistogramAdapter
 * @author pluk
 *
 */
public interface Histogram {
    Double getPercentile(double percentile);
    void recordValue(long value) throws HistogramException;
    void reset();
    long getTotalCount();
    long getSum();
    long getMax();
    long getMin();
    Double getStandardDeviation();
    Long getLast();
    Long getCountHigherThanValue(long value);
    byte[] encodeBase64();
}
