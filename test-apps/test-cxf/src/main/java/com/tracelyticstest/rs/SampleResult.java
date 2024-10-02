package com.tracelyticstest.rs;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "result")
@XmlAccessorType( XmlAccessType.FIELD)
public class SampleResult {
    public String path;
    public String method;
}
