package com.tracelytics.test;

import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown = true)
public class DataContainer {
    public Data data;
    
    public DataContainer() {
        // TODO Auto-generated constructor stub
    }
}
