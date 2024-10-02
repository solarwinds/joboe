/*
 * An XML document type.
 * Localname: ForecastReturn
 * Namespace: http://ws.cdyne.com/WeatherWS/
 * Java type: com.cdyne.ws.weatherws.ForecastReturnDocument
 *
 * Automatically generated - do not modify.
 */
package com.cdyne.ws.weatherws.impl;
/**
 * A document containing one ForecastReturn(@http://ws.cdyne.com/WeatherWS/) element.
 *
 * This is a complex type.
 */
public class ForecastReturnDocumentImpl extends org.apache.xmlbeans.impl.values.XmlComplexContentImpl implements com.cdyne.ws.weatherws.ForecastReturnDocument
{
    
    public ForecastReturnDocumentImpl(org.apache.xmlbeans.SchemaType sType)
    {
        super(sType);
    }
    
    private static final javax.xml.namespace.QName FORECASTRETURN$0 = 
        new javax.xml.namespace.QName("http://ws.cdyne.com/WeatherWS/", "ForecastReturn");
    
    
    /**
     * Gets the "ForecastReturn" element
     */
    public com.cdyne.ws.weatherws.ForecastReturn getForecastReturn()
    {
        synchronized (monitor())
        {
            check_orphaned();
            com.cdyne.ws.weatherws.ForecastReturn target = null;
            target = (com.cdyne.ws.weatherws.ForecastReturn)get_store().find_element_user(FORECASTRETURN$0, 0);
            if (target == null)
            {
                return null;
            }
            return target;
        }
    }
    
    /**
     * Tests for nil "ForecastReturn" element
     */
    public boolean isNilForecastReturn()
    {
        synchronized (monitor())
        {
            check_orphaned();
            com.cdyne.ws.weatherws.ForecastReturn target = null;
            target = (com.cdyne.ws.weatherws.ForecastReturn)get_store().find_element_user(FORECASTRETURN$0, 0);
            if (target == null) return false;
            return target.isNil();
        }
    }
    
    /**
     * Sets the "ForecastReturn" element
     */
    public void setForecastReturn(com.cdyne.ws.weatherws.ForecastReturn forecastReturn)
    {
        synchronized (monitor())
        {
            check_orphaned();
            com.cdyne.ws.weatherws.ForecastReturn target = null;
            target = (com.cdyne.ws.weatherws.ForecastReturn)get_store().find_element_user(FORECASTRETURN$0, 0);
            if (target == null)
            {
                target = (com.cdyne.ws.weatherws.ForecastReturn)get_store().add_element_user(FORECASTRETURN$0);
            }
            target.set(forecastReturn);
        }
    }
    
    /**
     * Appends and returns a new empty "ForecastReturn" element
     */
    public com.cdyne.ws.weatherws.ForecastReturn addNewForecastReturn()
    {
        synchronized (monitor())
        {
            check_orphaned();
            com.cdyne.ws.weatherws.ForecastReturn target = null;
            target = (com.cdyne.ws.weatherws.ForecastReturn)get_store().add_element_user(FORECASTRETURN$0);
            return target;
        }
    }
    
    /**
     * Nils the "ForecastReturn" element
     */
    public void setNilForecastReturn()
    {
        synchronized (monitor())
        {
            check_orphaned();
            com.cdyne.ws.weatherws.ForecastReturn target = null;
            target = (com.cdyne.ws.weatherws.ForecastReturn)get_store().find_element_user(FORECASTRETURN$0, 0);
            if (target == null)
            {
                target = (com.cdyne.ws.weatherws.ForecastReturn)get_store().add_element_user(FORECASTRETURN$0);
            }
            target.setNil();
        }
    }
}
