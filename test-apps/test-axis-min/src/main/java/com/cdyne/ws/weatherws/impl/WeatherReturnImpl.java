/*
 * XML Type:  WeatherReturn
 * Namespace: http://ws.cdyne.com/WeatherWS/
 * Java type: com.cdyne.ws.weatherws.WeatherReturn
 *
 * Automatically generated - do not modify.
 */
package com.cdyne.ws.weatherws.impl;
/**
 * An XML WeatherReturn(@http://ws.cdyne.com/WeatherWS/).
 *
 * This is a complex type.
 */
public class WeatherReturnImpl extends org.apache.xmlbeans.impl.values.XmlComplexContentImpl implements com.cdyne.ws.weatherws.WeatherReturn
{
    
    public WeatherReturnImpl(org.apache.xmlbeans.SchemaType sType)
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
    private static final javax.xml.namespace.QName WEATHERID$10 = 
        new javax.xml.namespace.QName("http://ws.cdyne.com/WeatherWS/", "WeatherID");
    private static final javax.xml.namespace.QName DESCRIPTION$12 = 
        new javax.xml.namespace.QName("http://ws.cdyne.com/WeatherWS/", "Description");
    private static final javax.xml.namespace.QName TEMPERATURE$14 = 
        new javax.xml.namespace.QName("http://ws.cdyne.com/WeatherWS/", "Temperature");
    private static final javax.xml.namespace.QName RELATIVEHUMIDITY$16 = 
        new javax.xml.namespace.QName("http://ws.cdyne.com/WeatherWS/", "RelativeHumidity");
    private static final javax.xml.namespace.QName WIND$18 = 
        new javax.xml.namespace.QName("http://ws.cdyne.com/WeatherWS/", "Wind");
    private static final javax.xml.namespace.QName PRESSURE$20 = 
        new javax.xml.namespace.QName("http://ws.cdyne.com/WeatherWS/", "Pressure");
    private static final javax.xml.namespace.QName VISIBILITY$22 = 
        new javax.xml.namespace.QName("http://ws.cdyne.com/WeatherWS/", "Visibility");
    private static final javax.xml.namespace.QName WINDCHILL$24 = 
        new javax.xml.namespace.QName("http://ws.cdyne.com/WeatherWS/", "WindChill");
    private static final javax.xml.namespace.QName REMARKS$26 = 
        new javax.xml.namespace.QName("http://ws.cdyne.com/WeatherWS/", "Remarks");
    
    
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
     * Gets the "WeatherID" element
     */
    public short getWeatherID()
    {
        synchronized (monitor())
        {
            check_orphaned();
            org.apache.xmlbeans.SimpleValue target = null;
            target = (org.apache.xmlbeans.SimpleValue)get_store().find_element_user(WEATHERID$10, 0);
            if (target == null)
            {
                return 0;
            }
            return target.getShortValue();
        }
    }
    
    /**
     * Gets (as xml) the "WeatherID" element
     */
    public org.apache.xmlbeans.XmlShort xgetWeatherID()
    {
        synchronized (monitor())
        {
            check_orphaned();
            org.apache.xmlbeans.XmlShort target = null;
            target = (org.apache.xmlbeans.XmlShort)get_store().find_element_user(WEATHERID$10, 0);
            return target;
        }
    }
    
    /**
     * Sets the "WeatherID" element
     */
    public void setWeatherID(short weatherID)
    {
        synchronized (monitor())
        {
            check_orphaned();
            org.apache.xmlbeans.SimpleValue target = null;
            target = (org.apache.xmlbeans.SimpleValue)get_store().find_element_user(WEATHERID$10, 0);
            if (target == null)
            {
                target = (org.apache.xmlbeans.SimpleValue)get_store().add_element_user(WEATHERID$10);
            }
            target.setShortValue(weatherID);
        }
    }
    
    /**
     * Sets (as xml) the "WeatherID" element
     */
    public void xsetWeatherID(org.apache.xmlbeans.XmlShort weatherID)
    {
        synchronized (monitor())
        {
            check_orphaned();
            org.apache.xmlbeans.XmlShort target = null;
            target = (org.apache.xmlbeans.XmlShort)get_store().find_element_user(WEATHERID$10, 0);
            if (target == null)
            {
                target = (org.apache.xmlbeans.XmlShort)get_store().add_element_user(WEATHERID$10);
            }
            target.set(weatherID);
        }
    }
    
    /**
     * Gets the "Description" element
     */
    public java.lang.String getDescription()
    {
        synchronized (monitor())
        {
            check_orphaned();
            org.apache.xmlbeans.SimpleValue target = null;
            target = (org.apache.xmlbeans.SimpleValue)get_store().find_element_user(DESCRIPTION$12, 0);
            if (target == null)
            {
                return null;
            }
            return target.getStringValue();
        }
    }
    
    /**
     * Gets (as xml) the "Description" element
     */
    public org.apache.xmlbeans.XmlString xgetDescription()
    {
        synchronized (monitor())
        {
            check_orphaned();
            org.apache.xmlbeans.XmlString target = null;
            target = (org.apache.xmlbeans.XmlString)get_store().find_element_user(DESCRIPTION$12, 0);
            return target;
        }
    }
    
    /**
     * True if has "Description" element
     */
    public boolean isSetDescription()
    {
        synchronized (monitor())
        {
            check_orphaned();
            return get_store().count_elements(DESCRIPTION$12) != 0;
        }
    }
    
    /**
     * Sets the "Description" element
     */
    public void setDescription(java.lang.String description)
    {
        synchronized (monitor())
        {
            check_orphaned();
            org.apache.xmlbeans.SimpleValue target = null;
            target = (org.apache.xmlbeans.SimpleValue)get_store().find_element_user(DESCRIPTION$12, 0);
            if (target == null)
            {
                target = (org.apache.xmlbeans.SimpleValue)get_store().add_element_user(DESCRIPTION$12);
            }
            target.setStringValue(description);
        }
    }
    
    /**
     * Sets (as xml) the "Description" element
     */
    public void xsetDescription(org.apache.xmlbeans.XmlString description)
    {
        synchronized (monitor())
        {
            check_orphaned();
            org.apache.xmlbeans.XmlString target = null;
            target = (org.apache.xmlbeans.XmlString)get_store().find_element_user(DESCRIPTION$12, 0);
            if (target == null)
            {
                target = (org.apache.xmlbeans.XmlString)get_store().add_element_user(DESCRIPTION$12);
            }
            target.set(description);
        }
    }
    
    /**
     * Unsets the "Description" element
     */
    public void unsetDescription()
    {
        synchronized (monitor())
        {
            check_orphaned();
            get_store().remove_element(DESCRIPTION$12, 0);
        }
    }
    
    /**
     * Gets the "Temperature" element
     */
    public java.lang.String getTemperature()
    {
        synchronized (monitor())
        {
            check_orphaned();
            org.apache.xmlbeans.SimpleValue target = null;
            target = (org.apache.xmlbeans.SimpleValue)get_store().find_element_user(TEMPERATURE$14, 0);
            if (target == null)
            {
                return null;
            }
            return target.getStringValue();
        }
    }
    
    /**
     * Gets (as xml) the "Temperature" element
     */
    public org.apache.xmlbeans.XmlString xgetTemperature()
    {
        synchronized (monitor())
        {
            check_orphaned();
            org.apache.xmlbeans.XmlString target = null;
            target = (org.apache.xmlbeans.XmlString)get_store().find_element_user(TEMPERATURE$14, 0);
            return target;
        }
    }
    
    /**
     * True if has "Temperature" element
     */
    public boolean isSetTemperature()
    {
        synchronized (monitor())
        {
            check_orphaned();
            return get_store().count_elements(TEMPERATURE$14) != 0;
        }
    }
    
    /**
     * Sets the "Temperature" element
     */
    public void setTemperature(java.lang.String temperature)
    {
        synchronized (monitor())
        {
            check_orphaned();
            org.apache.xmlbeans.SimpleValue target = null;
            target = (org.apache.xmlbeans.SimpleValue)get_store().find_element_user(TEMPERATURE$14, 0);
            if (target == null)
            {
                target = (org.apache.xmlbeans.SimpleValue)get_store().add_element_user(TEMPERATURE$14);
            }
            target.setStringValue(temperature);
        }
    }
    
    /**
     * Sets (as xml) the "Temperature" element
     */
    public void xsetTemperature(org.apache.xmlbeans.XmlString temperature)
    {
        synchronized (monitor())
        {
            check_orphaned();
            org.apache.xmlbeans.XmlString target = null;
            target = (org.apache.xmlbeans.XmlString)get_store().find_element_user(TEMPERATURE$14, 0);
            if (target == null)
            {
                target = (org.apache.xmlbeans.XmlString)get_store().add_element_user(TEMPERATURE$14);
            }
            target.set(temperature);
        }
    }
    
    /**
     * Unsets the "Temperature" element
     */
    public void unsetTemperature()
    {
        synchronized (monitor())
        {
            check_orphaned();
            get_store().remove_element(TEMPERATURE$14, 0);
        }
    }
    
    /**
     * Gets the "RelativeHumidity" element
     */
    public java.lang.String getRelativeHumidity()
    {
        synchronized (monitor())
        {
            check_orphaned();
            org.apache.xmlbeans.SimpleValue target = null;
            target = (org.apache.xmlbeans.SimpleValue)get_store().find_element_user(RELATIVEHUMIDITY$16, 0);
            if (target == null)
            {
                return null;
            }
            return target.getStringValue();
        }
    }
    
    /**
     * Gets (as xml) the "RelativeHumidity" element
     */
    public org.apache.xmlbeans.XmlString xgetRelativeHumidity()
    {
        synchronized (monitor())
        {
            check_orphaned();
            org.apache.xmlbeans.XmlString target = null;
            target = (org.apache.xmlbeans.XmlString)get_store().find_element_user(RELATIVEHUMIDITY$16, 0);
            return target;
        }
    }
    
    /**
     * True if has "RelativeHumidity" element
     */
    public boolean isSetRelativeHumidity()
    {
        synchronized (monitor())
        {
            check_orphaned();
            return get_store().count_elements(RELATIVEHUMIDITY$16) != 0;
        }
    }
    
    /**
     * Sets the "RelativeHumidity" element
     */
    public void setRelativeHumidity(java.lang.String relativeHumidity)
    {
        synchronized (monitor())
        {
            check_orphaned();
            org.apache.xmlbeans.SimpleValue target = null;
            target = (org.apache.xmlbeans.SimpleValue)get_store().find_element_user(RELATIVEHUMIDITY$16, 0);
            if (target == null)
            {
                target = (org.apache.xmlbeans.SimpleValue)get_store().add_element_user(RELATIVEHUMIDITY$16);
            }
            target.setStringValue(relativeHumidity);
        }
    }
    
    /**
     * Sets (as xml) the "RelativeHumidity" element
     */
    public void xsetRelativeHumidity(org.apache.xmlbeans.XmlString relativeHumidity)
    {
        synchronized (monitor())
        {
            check_orphaned();
            org.apache.xmlbeans.XmlString target = null;
            target = (org.apache.xmlbeans.XmlString)get_store().find_element_user(RELATIVEHUMIDITY$16, 0);
            if (target == null)
            {
                target = (org.apache.xmlbeans.XmlString)get_store().add_element_user(RELATIVEHUMIDITY$16);
            }
            target.set(relativeHumidity);
        }
    }
    
    /**
     * Unsets the "RelativeHumidity" element
     */
    public void unsetRelativeHumidity()
    {
        synchronized (monitor())
        {
            check_orphaned();
            get_store().remove_element(RELATIVEHUMIDITY$16, 0);
        }
    }
    
    /**
     * Gets the "Wind" element
     */
    public java.lang.String getWind()
    {
        synchronized (monitor())
        {
            check_orphaned();
            org.apache.xmlbeans.SimpleValue target = null;
            target = (org.apache.xmlbeans.SimpleValue)get_store().find_element_user(WIND$18, 0);
            if (target == null)
            {
                return null;
            }
            return target.getStringValue();
        }
    }
    
    /**
     * Gets (as xml) the "Wind" element
     */
    public org.apache.xmlbeans.XmlString xgetWind()
    {
        synchronized (monitor())
        {
            check_orphaned();
            org.apache.xmlbeans.XmlString target = null;
            target = (org.apache.xmlbeans.XmlString)get_store().find_element_user(WIND$18, 0);
            return target;
        }
    }
    
    /**
     * True if has "Wind" element
     */
    public boolean isSetWind()
    {
        synchronized (monitor())
        {
            check_orphaned();
            return get_store().count_elements(WIND$18) != 0;
        }
    }
    
    /**
     * Sets the "Wind" element
     */
    public void setWind(java.lang.String wind)
    {
        synchronized (monitor())
        {
            check_orphaned();
            org.apache.xmlbeans.SimpleValue target = null;
            target = (org.apache.xmlbeans.SimpleValue)get_store().find_element_user(WIND$18, 0);
            if (target == null)
            {
                target = (org.apache.xmlbeans.SimpleValue)get_store().add_element_user(WIND$18);
            }
            target.setStringValue(wind);
        }
    }
    
    /**
     * Sets (as xml) the "Wind" element
     */
    public void xsetWind(org.apache.xmlbeans.XmlString wind)
    {
        synchronized (monitor())
        {
            check_orphaned();
            org.apache.xmlbeans.XmlString target = null;
            target = (org.apache.xmlbeans.XmlString)get_store().find_element_user(WIND$18, 0);
            if (target == null)
            {
                target = (org.apache.xmlbeans.XmlString)get_store().add_element_user(WIND$18);
            }
            target.set(wind);
        }
    }
    
    /**
     * Unsets the "Wind" element
     */
    public void unsetWind()
    {
        synchronized (monitor())
        {
            check_orphaned();
            get_store().remove_element(WIND$18, 0);
        }
    }
    
    /**
     * Gets the "Pressure" element
     */
    public java.lang.String getPressure()
    {
        synchronized (monitor())
        {
            check_orphaned();
            org.apache.xmlbeans.SimpleValue target = null;
            target = (org.apache.xmlbeans.SimpleValue)get_store().find_element_user(PRESSURE$20, 0);
            if (target == null)
            {
                return null;
            }
            return target.getStringValue();
        }
    }
    
    /**
     * Gets (as xml) the "Pressure" element
     */
    public org.apache.xmlbeans.XmlString xgetPressure()
    {
        synchronized (monitor())
        {
            check_orphaned();
            org.apache.xmlbeans.XmlString target = null;
            target = (org.apache.xmlbeans.XmlString)get_store().find_element_user(PRESSURE$20, 0);
            return target;
        }
    }
    
    /**
     * True if has "Pressure" element
     */
    public boolean isSetPressure()
    {
        synchronized (monitor())
        {
            check_orphaned();
            return get_store().count_elements(PRESSURE$20) != 0;
        }
    }
    
    /**
     * Sets the "Pressure" element
     */
    public void setPressure(java.lang.String pressure)
    {
        synchronized (monitor())
        {
            check_orphaned();
            org.apache.xmlbeans.SimpleValue target = null;
            target = (org.apache.xmlbeans.SimpleValue)get_store().find_element_user(PRESSURE$20, 0);
            if (target == null)
            {
                target = (org.apache.xmlbeans.SimpleValue)get_store().add_element_user(PRESSURE$20);
            }
            target.setStringValue(pressure);
        }
    }
    
    /**
     * Sets (as xml) the "Pressure" element
     */
    public void xsetPressure(org.apache.xmlbeans.XmlString pressure)
    {
        synchronized (monitor())
        {
            check_orphaned();
            org.apache.xmlbeans.XmlString target = null;
            target = (org.apache.xmlbeans.XmlString)get_store().find_element_user(PRESSURE$20, 0);
            if (target == null)
            {
                target = (org.apache.xmlbeans.XmlString)get_store().add_element_user(PRESSURE$20);
            }
            target.set(pressure);
        }
    }
    
    /**
     * Unsets the "Pressure" element
     */
    public void unsetPressure()
    {
        synchronized (monitor())
        {
            check_orphaned();
            get_store().remove_element(PRESSURE$20, 0);
        }
    }
    
    /**
     * Gets the "Visibility" element
     */
    public java.lang.String getVisibility()
    {
        synchronized (monitor())
        {
            check_orphaned();
            org.apache.xmlbeans.SimpleValue target = null;
            target = (org.apache.xmlbeans.SimpleValue)get_store().find_element_user(VISIBILITY$22, 0);
            if (target == null)
            {
                return null;
            }
            return target.getStringValue();
        }
    }
    
    /**
     * Gets (as xml) the "Visibility" element
     */
    public org.apache.xmlbeans.XmlString xgetVisibility()
    {
        synchronized (monitor())
        {
            check_orphaned();
            org.apache.xmlbeans.XmlString target = null;
            target = (org.apache.xmlbeans.XmlString)get_store().find_element_user(VISIBILITY$22, 0);
            return target;
        }
    }
    
    /**
     * True if has "Visibility" element
     */
    public boolean isSetVisibility()
    {
        synchronized (monitor())
        {
            check_orphaned();
            return get_store().count_elements(VISIBILITY$22) != 0;
        }
    }
    
    /**
     * Sets the "Visibility" element
     */
    public void setVisibility(java.lang.String visibility)
    {
        synchronized (monitor())
        {
            check_orphaned();
            org.apache.xmlbeans.SimpleValue target = null;
            target = (org.apache.xmlbeans.SimpleValue)get_store().find_element_user(VISIBILITY$22, 0);
            if (target == null)
            {
                target = (org.apache.xmlbeans.SimpleValue)get_store().add_element_user(VISIBILITY$22);
            }
            target.setStringValue(visibility);
        }
    }
    
    /**
     * Sets (as xml) the "Visibility" element
     */
    public void xsetVisibility(org.apache.xmlbeans.XmlString visibility)
    {
        synchronized (monitor())
        {
            check_orphaned();
            org.apache.xmlbeans.XmlString target = null;
            target = (org.apache.xmlbeans.XmlString)get_store().find_element_user(VISIBILITY$22, 0);
            if (target == null)
            {
                target = (org.apache.xmlbeans.XmlString)get_store().add_element_user(VISIBILITY$22);
            }
            target.set(visibility);
        }
    }
    
    /**
     * Unsets the "Visibility" element
     */
    public void unsetVisibility()
    {
        synchronized (monitor())
        {
            check_orphaned();
            get_store().remove_element(VISIBILITY$22, 0);
        }
    }
    
    /**
     * Gets the "WindChill" element
     */
    public java.lang.String getWindChill()
    {
        synchronized (monitor())
        {
            check_orphaned();
            org.apache.xmlbeans.SimpleValue target = null;
            target = (org.apache.xmlbeans.SimpleValue)get_store().find_element_user(WINDCHILL$24, 0);
            if (target == null)
            {
                return null;
            }
            return target.getStringValue();
        }
    }
    
    /**
     * Gets (as xml) the "WindChill" element
     */
    public org.apache.xmlbeans.XmlString xgetWindChill()
    {
        synchronized (monitor())
        {
            check_orphaned();
            org.apache.xmlbeans.XmlString target = null;
            target = (org.apache.xmlbeans.XmlString)get_store().find_element_user(WINDCHILL$24, 0);
            return target;
        }
    }
    
    /**
     * True if has "WindChill" element
     */
    public boolean isSetWindChill()
    {
        synchronized (monitor())
        {
            check_orphaned();
            return get_store().count_elements(WINDCHILL$24) != 0;
        }
    }
    
    /**
     * Sets the "WindChill" element
     */
    public void setWindChill(java.lang.String windChill)
    {
        synchronized (monitor())
        {
            check_orphaned();
            org.apache.xmlbeans.SimpleValue target = null;
            target = (org.apache.xmlbeans.SimpleValue)get_store().find_element_user(WINDCHILL$24, 0);
            if (target == null)
            {
                target = (org.apache.xmlbeans.SimpleValue)get_store().add_element_user(WINDCHILL$24);
            }
            target.setStringValue(windChill);
        }
    }
    
    /**
     * Sets (as xml) the "WindChill" element
     */
    public void xsetWindChill(org.apache.xmlbeans.XmlString windChill)
    {
        synchronized (monitor())
        {
            check_orphaned();
            org.apache.xmlbeans.XmlString target = null;
            target = (org.apache.xmlbeans.XmlString)get_store().find_element_user(WINDCHILL$24, 0);
            if (target == null)
            {
                target = (org.apache.xmlbeans.XmlString)get_store().add_element_user(WINDCHILL$24);
            }
            target.set(windChill);
        }
    }
    
    /**
     * Unsets the "WindChill" element
     */
    public void unsetWindChill()
    {
        synchronized (monitor())
        {
            check_orphaned();
            get_store().remove_element(WINDCHILL$24, 0);
        }
    }
    
    /**
     * Gets the "Remarks" element
     */
    public java.lang.String getRemarks()
    {
        synchronized (monitor())
        {
            check_orphaned();
            org.apache.xmlbeans.SimpleValue target = null;
            target = (org.apache.xmlbeans.SimpleValue)get_store().find_element_user(REMARKS$26, 0);
            if (target == null)
            {
                return null;
            }
            return target.getStringValue();
        }
    }
    
    /**
     * Gets (as xml) the "Remarks" element
     */
    public org.apache.xmlbeans.XmlString xgetRemarks()
    {
        synchronized (monitor())
        {
            check_orphaned();
            org.apache.xmlbeans.XmlString target = null;
            target = (org.apache.xmlbeans.XmlString)get_store().find_element_user(REMARKS$26, 0);
            return target;
        }
    }
    
    /**
     * True if has "Remarks" element
     */
    public boolean isSetRemarks()
    {
        synchronized (monitor())
        {
            check_orphaned();
            return get_store().count_elements(REMARKS$26) != 0;
        }
    }
    
    /**
     * Sets the "Remarks" element
     */
    public void setRemarks(java.lang.String remarks)
    {
        synchronized (monitor())
        {
            check_orphaned();
            org.apache.xmlbeans.SimpleValue target = null;
            target = (org.apache.xmlbeans.SimpleValue)get_store().find_element_user(REMARKS$26, 0);
            if (target == null)
            {
                target = (org.apache.xmlbeans.SimpleValue)get_store().add_element_user(REMARKS$26);
            }
            target.setStringValue(remarks);
        }
    }
    
    /**
     * Sets (as xml) the "Remarks" element
     */
    public void xsetRemarks(org.apache.xmlbeans.XmlString remarks)
    {
        synchronized (monitor())
        {
            check_orphaned();
            org.apache.xmlbeans.XmlString target = null;
            target = (org.apache.xmlbeans.XmlString)get_store().find_element_user(REMARKS$26, 0);
            if (target == null)
            {
                target = (org.apache.xmlbeans.XmlString)get_store().add_element_user(REMARKS$26);
            }
            target.set(remarks);
        }
    }
    
    /**
     * Unsets the "Remarks" element
     */
    public void unsetRemarks()
    {
        synchronized (monitor())
        {
            check_orphaned();
            get_store().remove_element(REMARKS$26, 0);
        }
    }
}
