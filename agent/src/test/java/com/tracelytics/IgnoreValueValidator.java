package com.tracelytics;

public class IgnoreValueValidator implements ValueValidator<Object> {

    public boolean isValid(Object actualValue) {
        return true;
    }
    
    public String getValueString() {
        return "<ignored>";
    }
}
