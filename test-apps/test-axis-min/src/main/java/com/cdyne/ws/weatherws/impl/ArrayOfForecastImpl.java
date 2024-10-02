/*
 * XML Type:  ArrayOfForecast
 * Namespace: http://ws.cdyne.com/WeatherWS/
 * Java type: com.cdyne.ws.weatherws.ArrayOfForecast
 *
 * Automatically generated - do not modify.
 */
package com.cdyne.ws.weatherws.impl;
/**
 * An XML ArrayOfForecast(@http://ws.cdyne.com/WeatherWS/).
 *
 * This is a complex type.
 */
public class ArrayOfForecastImpl extends org.apache.xmlbeans.impl.values.XmlComplexContentImpl implements com.cdyne.ws.weatherws.ArrayOfForecast
{
    
    public ArrayOfForecastImpl(org.apache.xmlbeans.SchemaType sType)
    {
        super(sType);
    }
    
    private static final javax.xml.namespace.QName FORECAST$0 = 
        new javax.xml.namespace.QName("http://ws.cdyne.com/WeatherWS/", "Forecast");
    
    
    /**
     * Gets array of all "Forecast" elements
     */
    public com.cdyne.ws.weatherws.Forecast[] getForecastArray()
    {
        synchronized (monitor())
        {
            check_orphaned();
            java.util.List targetList = new java.util.ArrayList();
            get_store().find_all_element_users(FORECAST$0, targetList);
            com.cdyne.ws.weatherws.Forecast[] result = new com.cdyne.ws.weatherws.Forecast[targetList.size()];
            targetList.toArray(result);
            return result;
        }
    }
    
    /**
     * Gets ith "Forecast" element
     */
    public com.cdyne.ws.weatherws.Forecast getForecastArray(int i)
    {
        synchronized (monitor())
        {
            check_orphaned();
            com.cdyne.ws.weatherws.Forecast target = null;
            target = (com.cdyne.ws.weatherws.Forecast)get_store().find_element_user(FORECAST$0, i);
            if (target == null)
            {
                throw new IndexOutOfBoundsException();
            }
            return target;
        }
    }
    
    /**
     * Tests for nil ith "Forecast" element
     */
    public boolean isNilForecastArray(int i)
    {
        synchronized (monitor())
        {
            check_orphaned();
            com.cdyne.ws.weatherws.Forecast target = null;
            target = (com.cdyne.ws.weatherws.Forecast)get_store().find_element_user(FORECAST$0, i);
            if (target == null)
            {
                throw new IndexOutOfBoundsException();
            }
            return target.isNil();
        }
    }
    
    /**
     * Returns number of "Forecast" element
     */
    public int sizeOfForecastArray()
    {
        synchronized (monitor())
        {
            check_orphaned();
            return get_store().count_elements(FORECAST$0);
        }
    }
    
    /**
     * Sets array of all "Forecast" element
     */
    public void setForecastArray(com.cdyne.ws.weatherws.Forecast[] forecastArray)
    {
        synchronized (monitor())
        {
            check_orphaned();
            arraySetterHelper(forecastArray, FORECAST$0);
        }
    }
    
    /**
     * Sets ith "Forecast" element
     */
    public void setForecastArray(int i, com.cdyne.ws.weatherws.Forecast forecast)
    {
        synchronized (monitor())
        {
            check_orphaned();
            com.cdyne.ws.weatherws.Forecast target = null;
            target = (com.cdyne.ws.weatherws.Forecast)get_store().find_element_user(FORECAST$0, i);
            if (target == null)
            {
                throw new IndexOutOfBoundsException();
            }
            target.set(forecast);
        }
    }
    
    /**
     * Nils the ith "Forecast" element
     */
    public void setNilForecastArray(int i)
    {
        synchronized (monitor())
        {
            check_orphaned();
            com.cdyne.ws.weatherws.Forecast target = null;
            target = (com.cdyne.ws.weatherws.Forecast)get_store().find_element_user(FORECAST$0, i);
            if (target == null)
            {
                throw new IndexOutOfBoundsException();
            }
            target.setNil();
        }
    }
    
    /**
     * Inserts and returns a new empty value (as xml) as the ith "Forecast" element
     */
    public com.cdyne.ws.weatherws.Forecast insertNewForecast(int i)
    {
        synchronized (monitor())
        {
            check_orphaned();
            com.cdyne.ws.weatherws.Forecast target = null;
            target = (com.cdyne.ws.weatherws.Forecast)get_store().insert_element_user(FORECAST$0, i);
            return target;
        }
    }
    
    /**
     * Appends and returns a new empty value (as xml) as the last "Forecast" element
     */
    public com.cdyne.ws.weatherws.Forecast addNewForecast()
    {
        synchronized (monitor())
        {
            check_orphaned();
            com.cdyne.ws.weatherws.Forecast target = null;
            target = (com.cdyne.ws.weatherws.Forecast)get_store().add_element_user(FORECAST$0);
            return target;
        }
    }
    
    /**
     * Removes the ith "Forecast" element
     */
    public void removeForecast(int i)
    {
        synchronized (monitor())
        {
            check_orphaned();
            get_store().remove_element(FORECAST$0, i);
        }
    }
}
