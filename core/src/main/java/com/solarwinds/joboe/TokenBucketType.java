package com.solarwinds.joboe;

public enum TokenBucketType {
    REGULAR("regular"), STRICT("strict"), RELAXED("relaxed");

    private final String label;

    TokenBucketType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
