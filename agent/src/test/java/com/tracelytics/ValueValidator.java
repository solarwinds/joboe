package com.tracelytics;

public interface ValueValidator<T> {
    boolean isValid(T actualValue);
    
    String getValueString();
}
