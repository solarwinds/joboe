package com.tracelytics;

public class AnyValueValidator implements ValueValidator<Object> {
    public static final AnyValueValidator INSTANCE = new AnyValueValidator(); 

    public boolean isValid(Object actualValue) {
        return actualValue != null;
    }
    
    public String getValueString() {
        return "<any>";
    }
}
