/*
 * XML Type:  temp
 * Namespace: http://ws.cdyne.com/WeatherWS/
 * Java type: com.cdyne.ws.weatherws.Temp
 *
 * Automatically generated - do not modify.
 */
package com.cdyne.ws.weatherws.impl;
/**
 * An XML temp(@http://ws.cdyne.com/WeatherWS/).
 *
 * This is a complex type.
 */
public class TempImpl extends org.apache.xmlbeans.impl.values.XmlComplexContentImpl implements com.cdyne.ws.weatherws.Temp
{
    
    public TempImpl(org.apache.xmlbeans.SchemaType sType)
    {
        super(sType);
    }
    
    private static final javax.xml.namespace.QName MORNINGLOW$0 = 
        new javax.xml.namespace.QName("http://ws.cdyne.com/WeatherWS/", "MorningLow");
    private static final javax.xml.namespace.QName DAYTIMEHIGH$2 = 
        new javax.xml.namespace.QName("http://ws.cdyne.com/WeatherWS/", "DaytimeHigh");
    
    
    /**
     * Gets the "MorningLow" element
     */
    public java.lang.String getMorningLow()
    {
        synchronized (monitor())
        {
            check_orphaned();
            org.apache.xmlbeans.SimpleValue target = null;
            target = (org.apache.xmlbeans.SimpleValue)get_store().find_element_user(MORNINGLOW$0, 0);
            if (target == null)
            {
                return null;
            }
            return target.getStringValue();
        }
    }
    
    /**
     * Gets (as xml) the "MorningLow" element
     */
    public org.apache.xmlbeans.XmlString xgetMorningLow()
    {
        synchronized (monitor())
        {
            check_orphaned();
            org.apache.xmlbeans.XmlString target = null;
            target = (org.apache.xmlbeans.XmlString)get_store().find_element_user(MORNINGLOW$0, 0);
            return target;
        }
    }
    
    /**
     * True if has "MorningLow" element
     */
    public boolean isSetMorningLow()
    {
        synchronized (monitor())
        {
            check_orphaned();
            return get_store().count_elements(MORNINGLOW$0) != 0;
        }
    }
    
    /**
     * Sets the "MorningLow" element
     */
    public void setMorningLow(java.lang.String morningLow)
    {
        synchronized (monitor())
        {
            check_orphaned();
            org.apache.xmlbeans.SimpleValue target = null;
            target = (org.apache.xmlbeans.SimpleValue)get_store().find_element_user(MORNINGLOW$0, 0);
            if (target == null)
            {
                target = (org.apache.xmlbeans.SimpleValue)get_store().add_element_user(MORNINGLOW$0);
            }
            target.setStringValue(morningLow);
        }
    }
    
    /**
     * Sets (as xml) the "MorningLow" element
     */
    public void xsetMorningLow(org.apache.xmlbeans.XmlString morningLow)
    {
        synchronized (monitor())
        {
            check_orphaned();
            org.apache.xmlbeans.XmlString target = null;
            target = (org.apache.xmlbeans.XmlString)get_store().find_element_user(MORNINGLOW$0, 0);
            if (target == null)
            {
                target = (org.apache.xmlbeans.XmlString)get_store().add_element_user(MORNINGLOW$0);
            }
            target.set(morningLow);
        }
    }
    
    /**
     * Unsets the "MorningLow" element
     */
    public void unsetMorningLow()
    {
        synchronized (monitor())
        {
            check_orphaned();
            get_store().remove_element(MORNINGLOW$0, 0);
        }
    }
    
    /**
     * Gets the "DaytimeHigh" element
     */
    public java.lang.String getDaytimeHigh()
    {
        synchronized (monitor())
        {
            check_orphaned();
            org.apache.xmlbeans.SimpleValue target = null;
            target = (org.apache.xmlbeans.SimpleValue)get_store().find_element_user(DAYTIMEHIGH$2, 0);
            if (target == null)
            {
                return null;
            }
            return target.getStringValue();
        }
    }
    
    /**
     * Gets (as xml) the "DaytimeHigh" element
     */
    public org.apache.xmlbeans.XmlString xgetDaytimeHigh()
    {
        synchronized (monitor())
        {
            check_orphaned();
            org.apache.xmlbeans.XmlString target = null;
            target = (org.apache.xmlbeans.XmlString)get_store().find_element_user(DAYTIMEHIGH$2, 0);
            return target;
        }
    }
    
    /**
     * True if has "DaytimeHigh" element
     */
    public boolean isSetDaytimeHigh()
    {
        synchronized (monitor())
        {
            check_orphaned();
            return get_store().count_elements(DAYTIMEHIGH$2) != 0;
        }
    }
    
    /**
     * Sets the "DaytimeHigh" element
     */
    public void setDaytimeHigh(java.lang.String daytimeHigh)
    {
        synchronized (monitor())
        {
            check_orphaned();
            org.apache.xmlbeans.SimpleValue target = null;
            target = (org.apache.xmlbeans.SimpleValue)get_store().find_element_user(DAYTIMEHIGH$2, 0);
            if (target == null)
            {
                target = (org.apache.xmlbeans.SimpleValue)get_store().add_element_user(DAYTIMEHIGH$2);
            }
            target.setStringValue(daytimeHigh);
        }
    }
    
    /**
     * Sets (as xml) the "DaytimeHigh" element
     */
    public void xsetDaytimeHigh(org.apache.xmlbeans.XmlString daytimeHigh)
    {
        synchronized (monitor())
        {
            check_orphaned();
            org.apache.xmlbeans.XmlString target = null;
            target = (org.apache.xmlbeans.XmlString)get_store().find_element_user(DAYTIMEHIGH$2, 0);
            if (target == null)
            {
                target = (org.apache.xmlbeans.XmlString)get_store().add_element_user(DAYTIMEHIGH$2);
            }
            target.set(daytimeHigh);
        }
    }
    
    /**
     * Unsets the "DaytimeHigh" element
     */
    public void unsetDaytimeHigh()
    {
        synchronized (monitor())
        {
            check_orphaned();
            get_store().remove_element(DAYTIMEHIGH$2, 0);
        }
    }
}
