package com.solarwinds.metrics.histogram;

import com.solarwinds.metrics.hdrHistogram.ConcurrentHistogram;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.Base64;


/**
 * Adaptor to HDR histogram https://github.com/HdrHistogram/HdrHistogram
 * @author pluk
 *
 */
public class HdrHistogramAdapter implements Histogram {
    private final com.solarwinds.metrics.hdrHistogram.Histogram hdrHistogram;
    private Long lastValue;
    private final long MAX_VALUE;
    private final long MIN_VALUE = 0;
    
    HdrHistogramAdapter(long highestTrackableValue, int numberOfSignificantValueDigits) {
        hdrHistogram = new ConcurrentHistogram(highestTrackableValue, numberOfSignificantValueDigits);
        this.MAX_VALUE = highestTrackableValue;
    }
    
    public Double getPercentile(double percentile) {
        return (double) hdrHistogram.getValueAtPercentile(percentile);
    }

    public void recordValue(long value) throws HistogramException {
        
        if (value >= MIN_VALUE && value <= MAX_VALUE) {
            try {
                hdrHistogram.recordValue(value);
                lastValue = value;
            } catch (Exception e) {
                throw new HistogramException(e);
            }
        } else {
            throw new HistogramOutOfRangeException("Value " + value + " is rejected by the histogram as it is outside of acceptable range " + MIN_VALUE + " to " + MAX_VALUE);
        }
        
    }

    public void reset() {
        hdrHistogram.reset();
        lastValue = null;
    }

    public long getTotalCount() {
        return hdrHistogram.getTotalCount();
    }

    public long getSum() {
        return (long)(hdrHistogram.getTotalCount() * hdrHistogram.getMean()); //close enough... as this histogram does not keep total
    }

    public long getMax() {
        return hdrHistogram.getMaxValue();
    }

    public long getMin() {
        return hdrHistogram.getMinValue();
    }

    public Double getStandardDeviation() {
        return hdrHistogram.getStdDeviation();
    }

    public Long getLast() {
        return lastValue != null ? lastValue : null;
    }

    public Long getCountHigherThanValue(long value) {
        return hdrHistogram.getCountBetweenValues(value, hdrHistogram.getMaxValue());
    }

    com.solarwinds.metrics.hdrHistogram.Histogram getUnderlyingHistogram() {
         return hdrHistogram;
    }
    
    public byte[] encodeBase64() {
        int size = hdrHistogram.getNeededByteBufferCapacity();
        ByteBuffer buffer = ByteBuffer.allocate(size);
        
        hdrHistogram.encodeIntoCompressedByteBuffer(buffer);
        buffer.flip();  //cast for JDK 8- runtime compatibility
        
        byte[] serializedArray = new byte[buffer.remaining()];
        
        buffer.get(serializedArray);
        
        return Base64.getEncoder().encode(serializedArray);
    }
}
