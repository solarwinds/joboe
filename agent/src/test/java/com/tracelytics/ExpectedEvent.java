package com.tracelytics;

import java.util.HashMap;
import java.util.Map;

public class ExpectedEvent {
    private Map<String, Object> expectedEntries = new HashMap<String, Object>();
    
    public ExpectedEvent() {
        IgnoreValueValidator ignoreValueValidator = new IgnoreValueValidator();
        //all of the below entires can be safely ignored - doesn't care whether there's such a value
        addInfoWithValidator("X-TV-Meta", ignoreValueValidator);
        addInfoWithValidator("Edge", ignoreValueValidator);
        
        //all of the below entries should be present, but the value could be anything
        AnyValueValidator anyValueValidator = new AnyValueValidator();
        addInfoWithValidator("X-Trace", anyValueValidator);
        addInfoWithValidator("PID", anyValueValidator);
        addInfoWithValidator("Hostname", anyValueValidator);
        addInfoWithValidator("Timestamp_u", anyValueValidator);
        addInfoWithValidator("TID", anyValueValidator);
    }
    
    public ExpectedEvent(Object... keyObjectPairs) {
        if (keyObjectPairs.length % 2 != 0) {
            throw new IllegalArgumentException("keyObjectPairs should have even number of arguments");
        }
        
        for (int i = 0 ; i < keyObjectPairs.length; i += 2) {
            addInfo((String)keyObjectPairs[i], keyObjectPairs[i + 1]);
        }
    }
    
    public void addInfo(String key, Object value) {
        expectedEntries.put(key, value);
    }
    
    public void addInfoWithValidator(String key, ValueValidator<?> validator) {
        expectedEntries.put(key, validator);
    }
    
    public Map<String, Object> getExpectedEntries() {
        return expectedEntries;
    }
    
}


