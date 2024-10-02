/*
 * XML Type:  POP
 * Namespace: http://ws.cdyne.com/WeatherWS/
 * Java type: com.cdyne.ws.weatherws.POP
 *
 * Automatically generated - do not modify.
 */
package com.cdyne.ws.weatherws.impl;
/**
 * An XML POP(@http://ws.cdyne.com/WeatherWS/).
 *
 * This is a complex type.
 */
public class POPImpl extends org.apache.xmlbeans.impl.values.XmlComplexContentImpl implements com.cdyne.ws.weatherws.POP
{
    
    public POPImpl(org.apache.xmlbeans.SchemaType sType)
    {
        super(sType);
    }
    
    private static final javax.xml.namespace.QName NIGHTTIME$0 = 
        new javax.xml.namespace.QName("http://ws.cdyne.com/WeatherWS/", "Nighttime");
    private static final javax.xml.namespace.QName DAYTIME$2 = 
        new javax.xml.namespace.QName("http://ws.cdyne.com/WeatherWS/", "Daytime");
    
    
    /**
     * Gets the "Nighttime" element
     */
    public java.lang.String getNighttime()
    {
        synchronized (monitor())
        {
            check_orphaned();
            org.apache.xmlbeans.SimpleValue target = null;
            target = (org.apache.xmlbeans.SimpleValue)get_store().find_element_user(NIGHTTIME$0, 0);
            if (target == null)
            {
                return null;
            }
            return target.getStringValue();
        }
    }
    
    /**
     * Gets (as xml) the "Nighttime" element
     */
    public org.apache.xmlbeans.XmlString xgetNighttime()
    {
        synchronized (monitor())
        {
            check_orphaned();
            org.apache.xmlbeans.XmlString target = null;
            target = (org.apache.xmlbeans.XmlString)get_store().find_element_user(NIGHTTIME$0, 0);
            return target;
        }
    }
    
    /**
     * True if has "Nighttime" element
     */
    public boolean isSetNighttime()
    {
        synchronized (monitor())
        {
            check_orphaned();
            return get_store().count_elements(NIGHTTIME$0) != 0;
        }
    }
    
    /**
     * Sets the "Nighttime" element
     */
    public void setNighttime(java.lang.String nighttime)
    {
        synchronized (monitor())
        {
            check_orphaned();
            org.apache.xmlbeans.SimpleValue target = null;
            target = (org.apache.xmlbeans.SimpleValue)get_store().find_element_user(NIGHTTIME$0, 0);
            if (target == null)
            {
                target = (org.apache.xmlbeans.SimpleValue)get_store().add_element_user(NIGHTTIME$0);
            }
            target.setStringValue(nighttime);
        }
    }
    
    /**
     * Sets (as xml) the "Nighttime" element
     */
    public void xsetNighttime(org.apache.xmlbeans.XmlString nighttime)
    {
        synchronized (monitor())
        {
            check_orphaned();
            org.apache.xmlbeans.XmlString target = null;
            target = (org.apache.xmlbeans.XmlString)get_store().find_element_user(NIGHTTIME$0, 0);
            if (target == null)
            {
                target = (org.apache.xmlbeans.XmlString)get_store().add_element_user(NIGHTTIME$0);
            }
            target.set(nighttime);
        }
    }
    
    /**
     * Unsets the "Nighttime" element
     */
    public void unsetNighttime()
    {
        synchronized (monitor())
        {
            check_orphaned();
            get_store().remove_element(NIGHTTIME$0, 0);
        }
    }
    
    /**
     * Gets the "Daytime" element
     */
    public java.lang.String getDaytime()
    {
        synchronized (monitor())
        {
            check_orphaned();
            org.apache.xmlbeans.SimpleValue target = null;
            target = (org.apache.xmlbeans.SimpleValue)get_store().find_element_user(DAYTIME$2, 0);
            if (target == null)
            {
                return null;
            }
            return target.getStringValue();
        }
    }
    
    /**
     * Gets (as xml) the "Daytime" element
     */
    public org.apache.xmlbeans.XmlString xgetDaytime()
    {
        synchronized (monitor())
        {
            check_orphaned();
            org.apache.xmlbeans.XmlString target = null;
            target = (org.apache.xmlbeans.XmlString)get_store().find_element_user(DAYTIME$2, 0);
            return target;
        }
    }
    
    /**
     * True if has "Daytime" element
     */
    public boolean isSetDaytime()
    {
        synchronized (monitor())
        {
            check_orphaned();
            return get_store().count_elements(DAYTIME$2) != 0;
        }
    }
    
    /**
     * Sets the "Daytime" element
     */
    public void setDaytime(java.lang.String daytime)
    {
        synchronized (monitor())
        {
            check_orphaned();
            org.apache.xmlbeans.SimpleValue target = null;
            target = (org.apache.xmlbeans.SimpleValue)get_store().find_element_user(DAYTIME$2, 0);
            if (target == null)
            {
                target = (org.apache.xmlbeans.SimpleValue)get_store().add_element_user(DAYTIME$2);
            }
            target.setStringValue(daytime);
        }
    }
    
    /**
     * Sets (as xml) the "Daytime" element
     */
    public void xsetDaytime(org.apache.xmlbeans.XmlString daytime)
    {
        synchronized (monitor())
        {
            check_orphaned();
            org.apache.xmlbeans.XmlString target = null;
            target = (org.apache.xmlbeans.XmlString)get_store().find_element_user(DAYTIME$2, 0);
            if (target == null)
            {
                target = (org.apache.xmlbeans.XmlString)get_store().add_element_user(DAYTIME$2);
            }
            target.set(daytime);
        }
    }
    
    /**
     * Unsets the "Daytime" element
     */
    public void unsetDaytime()
    {
        synchronized (monitor())
        {
            check_orphaned();
            get_store().remove_element(DAYTIME$2, 0);
        }
    }
}
