package com.tracelyticstest.rs;

import java.math.BigDecimal;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "data")
@XmlAccessorType( XmlAccessType.FIELD)
public class Data {
    public BigDecimal count;
    public BigDecimal average;
    public BigDecimal latest;
       
}
