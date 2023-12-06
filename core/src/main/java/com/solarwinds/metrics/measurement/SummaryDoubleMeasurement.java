package com.solarwinds.metrics.measurement;

public class SummaryDoubleMeasurement extends SummaryMeasurement<Double> {
    private double sum;

    @Override
    protected void updateSum(Double value) {
        sum += value;
    }

    @Override
    public Double getSum() {
        return sum;
    }
}
