package com.solarwinds.joboe.metrics;

@FunctionalInterface
public interface TransactionNameOverFlowSupplier {
    boolean isLimitExceeded();
}
