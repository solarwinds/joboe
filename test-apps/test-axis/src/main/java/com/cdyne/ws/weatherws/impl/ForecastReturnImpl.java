/*
 * XML Type:  ForecastReturn
 * Namespace: http://ws.cdyne.com/WeatherWS/
 * Java type: com.cdyne.ws.weatherws.ForecastReturn
 *
 * Automatically generated - do not modify.
 */
package com.cdyne.ws.weatherws.impl;
/**
 * An XML ForecastReturn(@http://ws.cdyne.com/WeatherWS/).
 *
 * This is a complex type.
 */
public class ForecastReturnImpl extends org.apache.xmlbeans.impl.values.XmlComplexContentImpl implements com.cdyne.ws.weatherws.ForecastReturn
{
    
    public ForecastReturnImpl(org.apache.xmlbeans.SchemaType sType)
    {
        super(sType);
    }
    
    private static final javax.xml.namespace.QName SUCCESS$0 = 
        new javax.xml.namespace.QName("http://ws.cdyne.com/WeatherWS/", "Success");
    private static final javax.xml.namespace.QName RESPONSETEXT$2 = 
        new javax.xml.namespace.QName("http://ws.cdyne.com/WeatherWS/", "ResponseText");
    private static final javax.xml.namespace.QName STATE$4 = 
        new javax.xml.namespace.QName("http://ws.cdyne.com/WeatherWS/", "State");
    private static final javax.xml.namespace.QName CITY$6 = 
        new javax.xml.namespace.QName("http://ws.cdyne.com/WeatherWS/", "City");
    private static final javax.xml.namespace.QName WEATHERSTATIONCITY$8 = 
        new javax.xml.namespace.QName("http://ws.cdyne.com/WeatherWS/", "WeatherStationCity");
    private static final javax.xml.namespace.QName FORECASTRESULT$10 = 
        new javax.xml.namespace.QName("http://ws.cdyne.com/WeatherWS/", "ForecastResult");
    
    
    /**
     * Gets the "Success" element
     */
    public boolean getSuccess()
    {
        synchronized (monitor())
        {
            check_orphaned();
            org.apache.xmlbeans.SimpleValue target = null;
            target = (org.apache.xmlbeans.SimpleValue)get_store().find_element_user(SUCCESS$0, 0);
            if (target == null)
            {
                return false;
            }
            return target.getBooleanValue();
        }
    }
    
    /**
     * Gets (as xml) the "Success" element
     */
    public org.apache.xmlbeans.XmlBoolean xgetSuccess()
    {
        synchronized (monitor())
        {
            check_orphaned();
            org.apache.xmlbeans.XmlBoolean target = null;
            target = (org.apache.xmlbeans.XmlBoolean)get_store().find_element_user(SUCCESS$0, 0);
            return target;
        }
    }
    
    /**
     * Sets the "Success" element
     */
    public void setSuccess(boolean success)
    {
        synchronized (monitor())
        {
            check_orphaned();
            org.apache.xmlbeans.SimpleValue target = null;
            target = (org.apache.xmlbeans.SimpleValue)get_store().find_element_user(SUCCESS$0, 0);
            if (target == null)
            {
                target = (org.apache.xmlbeans.SimpleValue)get_store().add_element_user(SUCCESS$0);
            }
            target.setBooleanValue(success);
        }
    }
    
    /**
     * Sets (as xml) the "Success" element
     */
    public void xsetSuccess(org.apache.xmlbeans.XmlBoolean success)
    {
        synchronized (monitor())
        {
            check_orphaned();
            org.apache.xmlbeans.XmlBoolean target = null;
            target = (org.apache.xmlbeans.XmlBoolean)get_store().find_element_user(SUCCESS$0, 0);
            if (target == null)
            {
                target = (org.apache.xmlbeans.XmlBoolean)get_store().add_element_user(SUCCESS$0);
            }
            target.set(success);
        }
    }
    
    /**
     * Gets the "ResponseText" element
     */
    public java.lang.String getResponseText()
    {
        synchronized (monitor())
        {
            check_orphaned();
            org.apache.xmlbeans.SimpleValue target = null;
            target = (org.apache.xmlbeans.SimpleValue)get_store().find_element_user(RESPONSETEXT$2, 0);
            if (target == null)
            {
                return null;
            }
            return target.getStringValue();
        }
    }
    
    /**
     * Gets (as xml) the "ResponseText" element
     */
    public org.apache.xmlbeans.XmlString xgetResponseText()
    {
        synchronized (monitor())
        {
            check_orphaned();
            org.apache.xmlbeans.XmlString target = null;
            target = (org.apache.xmlbeans.XmlString)get_store().find_element_user(RESPONSETEXT$2, 0);
            return target;
        }
    }
    
    /**
     * True if has "ResponseText" element
     */
    public boolean isSetResponseText()
    {
        synchronized (monitor())
        {
            check_orphaned();
            return get_store().count_elements(RESPONSETEXT$2) != 0;
        }
    }
    
    /**
     * Sets the "ResponseText" element
     */
    public void setResponseText(java.lang.String responseText)
    {
        synchronized (monitor())
        {
            check_orphaned();
            org.apache.xmlbeans.SimpleValue target = null;
            target = (org.apache.xmlbeans.SimpleValue)get_store().find_element_user(RESPONSETEXT$2, 0);
            if (target == null)
            {
                target = (org.apache.xmlbeans.SimpleValue)get_store().add_element_user(RESPONSETEXT$2);
            }
            target.setStringValue(responseText);
        }
    }
    
    /**
     * Sets (as xml) the "ResponseText" element
     */
    public void xsetResponseText(org.apache.xmlbeans.XmlString responseText)
    {
        synchronized (monitor())
        {
            check_orphaned();
            org.apache.xmlbeans.XmlString target = null;
            target = (org.apache.xmlbeans.XmlString)get_store().find_element_user(RESPONSETEXT$2, 0);
            if (target == null)
            {
                target = (org.apache.xmlbeans.XmlString)get_store().add_element_user(RESPONSETEXT$2);
            }
            target.set(responseText);
        }
    }
    
    /**
     * Unsets the "ResponseText" element
     */
    public void unsetResponseText()
    {
        synchronized (monitor())
        {
            check_orphaned();
            get_store().remove_element(RESPONSETEXT$2, 0);
        }
    }
    
    /**
     * Gets the "State" element
     */
    public java.lang.String getState()
    {
        synchronized (monitor())
        {
            check_orphaned();
            org.apache.xmlbeans.SimpleValue target = null;
            target = (org.apache.xmlbeans.SimpleValue)get_store().find_element_user(STATE$4, 0);
            if (target == null)
            {
                return null;
            }
            return target.getStringValue();
        }
    }
    
    /**
     * Gets (as xml) the "State" element
     */
    public org.apache.xmlbeans.XmlString xgetState()
    {
        synchronized (monitor())
        {
            check_orphaned();
            org.apache.xmlbeans.XmlString target = null;
            target = (org.apache.xmlbeans.XmlString)get_store().find_element_user(STATE$4, 0);
            return target;
        }
    }
    
    /**
     * True if has "State" element
     */
    public boolean isSetState()
    {
        synchronized (monitor())
        {
            check_orphaned();
            return get_store().count_elements(STATE$4) != 0;
        }
    }
    
    /**
     * Sets the "State" element
     */
    public void setState(java.lang.String state)
    {
        synchronized (monitor())
        {
            check_orphaned();
            org.apache.xmlbeans.SimpleValue target = null;
            target = (org.apache.xmlbeans.SimpleValue)get_store().find_element_user(STATE$4, 0);
            if (target == null)
            {
                target = (org.apache.xmlbeans.SimpleValue)get_store().add_element_user(STATE$4);
            }
            target.setStringValue(state);
        }
    }
    
    /**
     * Sets (as xml) the "State" element
     */
    public void xsetState(org.apache.xmlbeans.XmlString state)
    {
        synchronized (monitor())
        {
            check_orphaned();
            org.apache.xmlbeans.XmlString target = null;
            target = (org.apache.xmlbeans.XmlString)get_store().find_element_user(STATE$4, 0);
            if (target == null)
            {
                target = (org.apache.xmlbeans.XmlString)get_store().add_element_user(STATE$4);
            }
            target.set(state);
        }
    }
    
    /**
     * Unsets the "State" element
     */
    public void unsetState()
    {
        synchronized (monitor())
        {
            check_orphaned();
            get_store().remove_element(STATE$4, 0);
        }
    }
    
    /**
     * Gets the "City" element
     */
    public java.lang.String getCity()
    {
        synchronized (monitor())
        {
            check_orphaned();
            org.apache.xmlbeans.SimpleValue target = null;
            target = (org.apache.xmlbeans.SimpleValue)get_store().find_element_user(CITY$6, 0);
            if (target == null)
            {
                return null;
            }
            return target.getStringValue();
        }
    }
    
    /**
     * Gets (as xml) the "City" element
     */
    public org.apache.xmlbeans.XmlString xgetCity()
    {
        synchronized (monitor())
        {
            check_orphaned();
            org.apache.xmlbeans.XmlString target = null;
            target = (org.apache.xmlbeans.XmlString)get_store().find_element_user(CITY$6, 0);
            return target;
        }
    }
    
    /**
     * True if has "City" element
     */
    public boolean isSetCity()
    {
        synchronized (monitor())
        {
            check_orphaned();
            return get_store().count_elements(CITY$6) != 0;
        }
    }
    
    /**
     * Sets the "City" element
     */
    public void setCity(java.lang.String city)
    {
        synchronized (monitor())
        {
            check_orphaned();
            org.apache.xmlbeans.SimpleValue target = null;
            target = (org.apache.xmlbeans.SimpleValue)get_store().find_element_user(CITY$6, 0);
            if (target == null)
            {
                target = (org.apache.xmlbeans.SimpleValue)get_store().add_element_user(CITY$6);
            }
            target.setStringValue(city);
        }
    }
    
    /**
     * Sets (as xml) the "City" element
     */
    public void xsetCity(org.apache.xmlbeans.XmlString city)
    {
        synchronized (monitor())
        {
            check_orphaned();
            org.apache.xmlbeans.XmlString target = null;
            target = (org.apache.xmlbeans.XmlString)get_store().find_element_user(CITY$6, 0);
            if (target == null)
            {
                target = (org.apache.xmlbeans.XmlString)get_store().add_element_user(CITY$6);
            }
            target.set(city);
        }
    }
    
    /**
     * Unsets the "City" element
     */
    public void unsetCity()
    {
        synchronized (monitor())
        {
            check_orphaned();
            get_store().remove_element(CITY$6, 0);
        }
    }
    
    /**
     * Gets the "WeatherStationCity" element
     */
    public java.lang.String getWeatherStationCity()
    {
        synchronized (monitor())
        {
            check_orphaned();
            org.apache.xmlbeans.SimpleValue target = null;
            target = (org.apache.xmlbeans.SimpleValue)get_store().find_element_user(WEATHERSTATIONCITY$8, 0);
            if (target == null)
            {
                return null;
            }
            return target.getStringValue();
        }
    }
    
    /**
     * Gets (as xml) the "WeatherStationCity" element
     */
    public org.apache.xmlbeans.XmlString xgetWeatherStationCity()
    {
        synchronized (monitor())
        {
            check_orphaned();
            org.apache.xmlbeans.XmlString target = null;
            target = (org.apache.xmlbeans.XmlString)get_store().find_element_user(WEATHERSTATIONCITY$8, 0);
            return target;
        }
    }
    
    /**
     * True if has "WeatherStationCity" element
     */
    public boolean isSetWeatherStationCity()
    {
        synchronized (monitor())
        {
            check_orphaned();
            return get_store().count_elements(WEATHERSTATIONCITY$8) != 0;
        }
    }
    
    /**
     * Sets the "WeatherStationCity" element
     */
    public void setWeatherStationCity(java.lang.String weatherStationCity)
    {
        synchronized (monitor())
        {
            check_orphaned();
            org.apache.xmlbeans.SimpleValue target = null;
            target = (org.apache.xmlbeans.SimpleValue)get_store().find_element_user(WEATHERSTATIONCITY$8, 0);
            if (target == null)
            {
                target = (org.apache.xmlbeans.SimpleValue)get_store().add_element_user(WEATHERSTATIONCITY$8);
            }
            target.setStringValue(weatherStationCity);
        }
    }
    
    /**
     * Sets (as xml) the "WeatherStationCity" element
     */
    public void xsetWeatherStationCity(org.apache.xmlbeans.XmlString weatherStationCity)
    {
        synchronized (monitor())
        {
            check_orphaned();
            org.apache.xmlbeans.XmlString target = null;
            target = (org.apache.xmlbeans.XmlString)get_store().find_element_user(WEATHERSTATIONCITY$8, 0);
            if (target == null)
            {
                target = (org.apache.xmlbeans.XmlString)get_store().add_element_user(WEATHERSTATIONCITY$8);
            }
            target.set(weatherStationCity);
        }
    }
    
    /**
     * Unsets the "WeatherStationCity" element
     */
    public void unsetWeatherStationCity()
    {
        synchronized (monitor())
        {
            check_orphaned();
            get_store().remove_element(WEATHERSTATIONCITY$8, 0);
        }
    }
    
    /**
     * Gets the "ForecastResult" element
     */
    public com.cdyne.ws.weatherws.ArrayOfForecast getForecastResult()
    {
        synchronized (monitor())
        {
            check_orphaned();
            com.cdyne.ws.weatherws.ArrayOfForecast target = null;
            target = (com.cdyne.ws.weatherws.ArrayOfForecast)get_store().find_element_user(FORECASTRESULT$10, 0);
            if (target == null)
            {
                return null;
            }
            return target;
        }
    }
    
    /**
     * True if has "ForecastResult" element
     */
    public boolean isSetForecastResult()
    {
        synchronized (monitor())
        {
            check_orphaned();
            return get_store().count_elements(FORECASTRESULT$10) != 0;
        }
    }
    
    /**
     * Sets the "ForecastResult" element
     */
    public void setForecastResult(com.cdyne.ws.weatherws.ArrayOfForecast forecastResult)
    {
        synchronized (monitor())
        {
            check_orphaned();
            com.cdyne.ws.weatherws.ArrayOfForecast target = null;
            target = (com.cdyne.ws.weatherws.ArrayOfForecast)get_store().find_element_user(FORECASTRESULT$10, 0);
            if (target == null)
            {
                target = (com.cdyne.ws.weatherws.ArrayOfForecast)get_store().add_element_user(FORECASTRESULT$10);
            }
            target.set(forecastResult);
        }
    }
    
    /**
     * Appends and returns a new empty "ForecastResult" element
     */
    public com.cdyne.ws.weatherws.ArrayOfForecast addNewForecastResult()
    {
        synchronized (monitor())
        {
            check_orphaned();
            com.cdyne.ws.weatherws.ArrayOfForecast target = null;
            target = (com.cdyne.ws.weatherws.ArrayOfForecast)get_store().add_element_user(FORECASTRESULT$10);
            return target;
        }
    }
    
    /**
     * Unsets the "ForecastResult" element
     */
    public void unsetForecastResult()
    {
        synchronized (monitor())
        {
            check_orphaned();
            get_store().remove_element(FORECASTRESULT$10, 0);
        }
    }
}
