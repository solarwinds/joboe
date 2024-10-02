/*
 * XML Type:  ArrayOfWeatherDescription
 * Namespace: http://ws.cdyne.com/WeatherWS/
 * Java type: com.cdyne.ws.weatherws.ArrayOfWeatherDescription
 *
 * Automatically generated - do not modify.
 */
package com.cdyne.ws.weatherws.impl;
/**
 * An XML ArrayOfWeatherDescription(@http://ws.cdyne.com/WeatherWS/).
 *
 * This is a complex type.
 */
public class ArrayOfWeatherDescriptionImpl extends org.apache.xmlbeans.impl.values.XmlComplexContentImpl implements com.cdyne.ws.weatherws.ArrayOfWeatherDescription
{
    
    public ArrayOfWeatherDescriptionImpl(org.apache.xmlbeans.SchemaType sType)
    {
        super(sType);
    }
    
    private static final javax.xml.namespace.QName WEATHERDESCRIPTION$0 = 
        new javax.xml.namespace.QName("http://ws.cdyne.com/WeatherWS/", "WeatherDescription");
    
    
    /**
     * Gets array of all "WeatherDescription" elements
     */
    public com.cdyne.ws.weatherws.WeatherDescription[] getWeatherDescriptionArray()
    {
        synchronized (monitor())
        {
            check_orphaned();
            java.util.List targetList = new java.util.ArrayList();
            get_store().find_all_element_users(WEATHERDESCRIPTION$0, targetList);
            com.cdyne.ws.weatherws.WeatherDescription[] result = new com.cdyne.ws.weatherws.WeatherDescription[targetList.size()];
            targetList.toArray(result);
            return result;
        }
    }
    
    /**
     * Gets ith "WeatherDescription" element
     */
    public com.cdyne.ws.weatherws.WeatherDescription getWeatherDescriptionArray(int i)
    {
        synchronized (monitor())
        {
            check_orphaned();
            com.cdyne.ws.weatherws.WeatherDescription target = null;
            target = (com.cdyne.ws.weatherws.WeatherDescription)get_store().find_element_user(WEATHERDESCRIPTION$0, i);
            if (target == null)
            {
                throw new IndexOutOfBoundsException();
            }
            return target;
        }
    }
    
    /**
     * Returns number of "WeatherDescription" element
     */
    public int sizeOfWeatherDescriptionArray()
    {
        synchronized (monitor())
        {
            check_orphaned();
            return get_store().count_elements(WEATHERDESCRIPTION$0);
        }
    }
    
    /**
     * Sets array of all "WeatherDescription" element
     */
    public void setWeatherDescriptionArray(com.cdyne.ws.weatherws.WeatherDescription[] weatherDescriptionArray)
    {
        synchronized (monitor())
        {
            check_orphaned();
            arraySetterHelper(weatherDescriptionArray, WEATHERDESCRIPTION$0);
        }
    }
    
    /**
     * Sets ith "WeatherDescription" element
     */
    public void setWeatherDescriptionArray(int i, com.cdyne.ws.weatherws.WeatherDescription weatherDescription)
    {
        synchronized (monitor())
        {
            check_orphaned();
            com.cdyne.ws.weatherws.WeatherDescription target = null;
            target = (com.cdyne.ws.weatherws.WeatherDescription)get_store().find_element_user(WEATHERDESCRIPTION$0, i);
            if (target == null)
            {
                throw new IndexOutOfBoundsException();
            }
            target.set(weatherDescription);
        }
    }
    
    /**
     * Inserts and returns a new empty value (as xml) as the ith "WeatherDescription" element
     */
    public com.cdyne.ws.weatherws.WeatherDescription insertNewWeatherDescription(int i)
    {
        synchronized (monitor())
        {
            check_orphaned();
            com.cdyne.ws.weatherws.WeatherDescription target = null;
            target = (com.cdyne.ws.weatherws.WeatherDescription)get_store().insert_element_user(WEATHERDESCRIPTION$0, i);
            return target;
        }
    }
    
    /**
     * Appends and returns a new empty value (as xml) as the last "WeatherDescription" element
     */
    public com.cdyne.ws.weatherws.WeatherDescription addNewWeatherDescription()
    {
        synchronized (monitor())
        {
            check_orphaned();
            com.cdyne.ws.weatherws.WeatherDescription target = null;
            target = (com.cdyne.ws.weatherws.WeatherDescription)get_store().add_element_user(WEATHERDESCRIPTION$0);
            return target;
        }
    }
    
    /**
     * Removes the ith "WeatherDescription" element
     */
    public void removeWeatherDescription(int i)
    {
        synchronized (monitor())
        {
            check_orphaned();
            get_store().remove_element(WEATHERDESCRIPTION$0, i);
        }
    }
}
