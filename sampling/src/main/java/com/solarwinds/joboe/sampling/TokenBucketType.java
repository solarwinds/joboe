package com.solarwinds.joboe.sampling;

import lombok.Getter;

@Getter
public enum TokenBucketType {
    REGULAR("regular"), STRICT("strict"), RELAXED("relaxed");

    private final String label;

    TokenBucketType(String label) {
        this.label = label;
    }

}
