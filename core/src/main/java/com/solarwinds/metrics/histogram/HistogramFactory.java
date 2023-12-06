package com.solarwinds.metrics.histogram;

/**
 * Builds concrete histogram based on {@link HistogramType} provided
 * @author pluk
 *
 */
public class HistogramFactory {
    public enum HistogramType { HDR }
    static final int NUMBER_OF_SIGNIFICANT_VALUE_DIGITS = 3;

    public static final Histogram buildHistogram(HistogramType histogramType, long maxValue) {
        if (histogramType == HistogramType.HDR) {
            return new HdrHistogramAdapter(maxValue, NUMBER_OF_SIGNIFICANT_VALUE_DIGITS);
        } else {
            return null;
        }
    }
}
