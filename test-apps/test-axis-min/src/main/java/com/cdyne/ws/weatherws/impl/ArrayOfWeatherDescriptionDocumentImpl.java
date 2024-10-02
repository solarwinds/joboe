/*
 * An XML document type.
 * Localname: ArrayOfWeatherDescription
 * Namespace: http://ws.cdyne.com/WeatherWS/
 * Java type: com.cdyne.ws.weatherws.ArrayOfWeatherDescriptionDocument
 *
 * Automatically generated - do not modify.
 */
package com.cdyne.ws.weatherws.impl;
/**
 * A document containing one ArrayOfWeatherDescription(@http://ws.cdyne.com/WeatherWS/) element.
 *
 * This is a complex type.
 */
public class ArrayOfWeatherDescriptionDocumentImpl extends org.apache.xmlbeans.impl.values.XmlComplexContentImpl implements com.cdyne.ws.weatherws.ArrayOfWeatherDescriptionDocument
{
    
    public ArrayOfWeatherDescriptionDocumentImpl(org.apache.xmlbeans.SchemaType sType)
    {
        super(sType);
    }
    
    private static final javax.xml.namespace.QName ARRAYOFWEATHERDESCRIPTION$0 = 
        new javax.xml.namespace.QName("http://ws.cdyne.com/WeatherWS/", "ArrayOfWeatherDescription");
    
    
    /**
     * Gets the "ArrayOfWeatherDescription" element
     */
    public com.cdyne.ws.weatherws.ArrayOfWeatherDescription getArrayOfWeatherDescription()
    {
        synchronized (monitor())
        {
            check_orphaned();
            com.cdyne.ws.weatherws.ArrayOfWeatherDescription target = null;
            target = (com.cdyne.ws.weatherws.ArrayOfWeatherDescription)get_store().find_element_user(ARRAYOFWEATHERDESCRIPTION$0, 0);
            if (target == null)
            {
                return null;
            }
            return target;
        }
    }
    
    /**
     * Tests for nil "ArrayOfWeatherDescription" element
     */
    public boolean isNilArrayOfWeatherDescription()
    {
        synchronized (monitor())
        {
            check_orphaned();
            com.cdyne.ws.weatherws.ArrayOfWeatherDescription target = null;
            target = (com.cdyne.ws.weatherws.ArrayOfWeatherDescription)get_store().find_element_user(ARRAYOFWEATHERDESCRIPTION$0, 0);
            if (target == null) return false;
            return target.isNil();
        }
    }
    
    /**
     * Sets the "ArrayOfWeatherDescription" element
     */
    public void setArrayOfWeatherDescription(com.cdyne.ws.weatherws.ArrayOfWeatherDescription arrayOfWeatherDescription)
    {
        synchronized (monitor())
        {
            check_orphaned();
            com.cdyne.ws.weatherws.ArrayOfWeatherDescription target = null;
            target = (com.cdyne.ws.weatherws.ArrayOfWeatherDescription)get_store().find_element_user(ARRAYOFWEATHERDESCRIPTION$0, 0);
            if (target == null)
            {
                target = (com.cdyne.ws.weatherws.ArrayOfWeatherDescription)get_store().add_element_user(ARRAYOFWEATHERDESCRIPTION$0);
            }
            target.set(arrayOfWeatherDescription);
        }
    }
    
    /**
     * Appends and returns a new empty "ArrayOfWeatherDescription" element
     */
    public com.cdyne.ws.weatherws.ArrayOfWeatherDescription addNewArrayOfWeatherDescription()
    {
        synchronized (monitor())
        {
            check_orphaned();
            com.cdyne.ws.weatherws.ArrayOfWeatherDescription target = null;
            target = (com.cdyne.ws.weatherws.ArrayOfWeatherDescription)get_store().add_element_user(ARRAYOFWEATHERDESCRIPTION$0);
            return target;
        }
    }
    
    /**
     * Nils the "ArrayOfWeatherDescription" element
     */
    public void setNilArrayOfWeatherDescription()
    {
        synchronized (monitor())
        {
            check_orphaned();
            com.cdyne.ws.weatherws.ArrayOfWeatherDescription target = null;
            target = (com.cdyne.ws.weatherws.ArrayOfWeatherDescription)get_store().find_element_user(ARRAYOFWEATHERDESCRIPTION$0, 0);
            if (target == null)
            {
                target = (com.cdyne.ws.weatherws.ArrayOfWeatherDescription)get_store().add_element_user(ARRAYOFWEATHERDESCRIPTION$0);
            }
            target.setNil();
        }
    }
}
