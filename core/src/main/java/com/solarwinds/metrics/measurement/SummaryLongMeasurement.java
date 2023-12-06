package com.solarwinds.metrics.measurement;

public class SummaryLongMeasurement extends SummaryMeasurement<Long> {
    private long sum;

    @Override
    protected void updateSum(Long value) {
        sum += value;
    }

    @Override
    public Long getSum() {
        return sum;
    }
}
