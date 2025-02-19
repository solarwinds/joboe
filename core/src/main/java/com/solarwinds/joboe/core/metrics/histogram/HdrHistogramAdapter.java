package com.solarwinds.joboe.core.metrics.histogram;

import com.solarwinds.joboe.core.metrics.hdrHistogram.ConcurrentHistogram;

import java.nio.ByteBuffer;
import java.util.Base64;


/**
 * Adaptor to HDR histogram <a href="https://github.com/HdrHistogram/HdrHistogram">...</a>
 * @author pluk
 *
 */
public class HdrHistogramAdapter implements Histogram {
    private final com.solarwinds.joboe.core.metrics.hdrHistogram.Histogram hdrHistogram;
    private Long lastValue;
    private final long MAX_VALUE;
    private final long MIN_VALUE = 0;
    
    HdrHistogramAdapter(long highestTrackableValue, int numberOfSignificantValueDigits) {
        hdrHistogram = new ConcurrentHistogram(highestTrackableValue, numberOfSignificantValueDigits);
        this.MAX_VALUE = highestTrackableValue;
    }
    
    @Override
    public Double getPercentile(double percentile) {
        return (double) hdrHistogram.getValueAtPercentile(percentile);
    }

    @Override
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

    @Override
    public void reset() {
        hdrHistogram.reset();
        lastValue = null;
    }

    @Override
    public long getTotalCount() {
        return hdrHistogram.getTotalCount();
    }

    @Override
    public long getSum() {
        return (long)(hdrHistogram.getTotalCount() * hdrHistogram.getMean()); //close enough... as this histogram does not keep total
    }

    @Override
    public long getMax() {
        return hdrHistogram.getMaxValue();
    }

    @Override
    public long getMin() {
        return hdrHistogram.getMinValue();
    }

    @Override
    public Double getStandardDeviation() {
        return hdrHistogram.getStdDeviation();
    }

    @Override
    public Long getLast() {
        return lastValue != null ? lastValue : null;
    }

    @Override
    public Long getCountHigherThanValue(long value) {
        return hdrHistogram.getCountBetweenValues(value, hdrHistogram.getMaxValue());
    }

    com.solarwinds.joboe.core.metrics.hdrHistogram.Histogram getUnderlyingHistogram() {
         return hdrHistogram;
    }
    
    @Override
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
