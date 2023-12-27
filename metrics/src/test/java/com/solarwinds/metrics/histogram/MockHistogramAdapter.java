package com.solarwinds.metrics.histogram;

public class MockHistogramAdapter implements Histogram {
    private final Double percentile;
    @lombok.Getter(onMethod_ = {@Override})
    private final long totalCount;
    @lombok.Getter(onMethod_ = {@Override})
    private final long sum;
    @lombok.Getter(onMethod_ = {@Override})
    private final long max;
    @lombok.Getter(onMethod_ = {@Override})
    private final long min;
    @lombok.Getter(onMethod_ = {@Override})
    private final Double standardDeviation;
    @lombok.Getter(onMethod_ = {@Override})
    private final Long last;
    private final Long countHigherThanValue;
    
    public MockHistogramAdapter(Double percentile, long totalCount, long sum, long max, long min, Double standardDeviation, long last, long countHigherThanValue) {
        super();
        this.percentile = percentile;
        this.totalCount = totalCount;
        this.sum = sum;
        this.max = max;
        this.min = min;
        this.standardDeviation = standardDeviation;
        this.last = last;
        this.countHigherThanValue = countHigherThanValue;
    }
    
    @Override
    public Double getPercentile(double percentile) {
        return percentile;
    }

    @Override
    public Long getCountHigherThanValue(long value) {
        return countHigherThanValue;
    }
    
    @Override
    public void recordValue(long value) {
        // TODO Auto-generated method stub
        
    }
    @Override
    public void reset() {
        // TODO Auto-generated method stub
        
    }
    
    
    

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((countHigherThanValue == null) ? 0 : countHigherThanValue.hashCode());
        result = prime * result + ((last == null) ? 0 : last.hashCode());
        result = prime * result + (int) (max ^ (max >>> 32));
        result = prime * result + (int) (min ^ (min >>> 32));
        result = prime * result + ((percentile == null) ? 0 : percentile.hashCode());
        result = prime * result + ((standardDeviation == null) ? 0 : standardDeviation.hashCode());
        result = prime * result + (int) (sum ^ (sum >>> 32));
        result = prime * result + (int) (totalCount ^ (totalCount >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        MockHistogramAdapter other = (MockHistogramAdapter) obj;
        if (countHigherThanValue == null) {
            if (other.countHigherThanValue != null)
                return false;
        } else if (!countHigherThanValue.equals(other.countHigherThanValue))
            return false;
        if (last == null) {
            if (other.last != null)
                return false;
        } else if (!last.equals(other.last))
            return false;
        if (max != other.max)
            return false;
        if (min != other.min)
            return false;
        if (percentile == null) {
            if (other.percentile != null)
                return false;
        } else if (!percentile.equals(other.percentile))
            return false;
        if (standardDeviation == null) {
            if (other.standardDeviation != null)
                return false;
        } else if (!standardDeviation.equals(other.standardDeviation))
            return false;
        if (sum != other.sum)
            return false;
        return totalCount == other.totalCount;
    }

    @Override
    public byte[] encodeBase64() {
        return new byte[0];
    }
    
    
}
