package com.tracelytics.test;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DataContainer {
    public Data data;
    
    public DataContainer() {
        // TODO Auto-generated constructor stub
    }
}
