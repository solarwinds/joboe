/*
 * XML Type:  WeatherDescription
 * Namespace: http://ws.cdyne.com/WeatherWS/
 * Java type: com.cdyne.ws.weatherws.WeatherDescription
 *
 * Automatically generated - do not modify.
 */
package com.cdyne.ws.weatherws;


/**
 * An XML WeatherDescription(@http://ws.cdyne.com/WeatherWS/).
 *
 * This is a complex type.
 */
public interface WeatherDescription extends org.apache.xmlbeans.XmlObject
{
    public static final org.apache.xmlbeans.SchemaType type = (org.apache.xmlbeans.SchemaType)
        org.apache.xmlbeans.XmlBeans.typeSystemForClassLoader(WeatherDescription.class.getClassLoader(), "schemaorg_apache_xmlbeans.system.s9BF98892DFFBF58E7682C993B0C16982").resolveHandle("weatherdescription5b9btype");
    
    /**
     * Gets the "WeatherID" element
     */
    short getWeatherID();
    
    /**
     * Gets (as xml) the "WeatherID" element
     */
    org.apache.xmlbeans.XmlShort xgetWeatherID();
    
    /**
     * Sets the "WeatherID" element
     */
    void setWeatherID(short weatherID);
    
    /**
     * Sets (as xml) the "WeatherID" element
     */
    void xsetWeatherID(org.apache.xmlbeans.XmlShort weatherID);
    
    /**
     * Gets the "Description" element
     */
    java.lang.String getDescription();
    
    /**
     * Gets (as xml) the "Description" element
     */
    org.apache.xmlbeans.XmlString xgetDescription();
    
    /**
     * True if has "Description" element
     */
    boolean isSetDescription();
    
    /**
     * Sets the "Description" element
     */
    void setDescription(java.lang.String description);
    
    /**
     * Sets (as xml) the "Description" element
     */
    void xsetDescription(org.apache.xmlbeans.XmlString description);
    
    /**
     * Unsets the "Description" element
     */
    void unsetDescription();
    
    /**
     * Gets the "PictureURL" element
     */
    java.lang.String getPictureURL();
    
    /**
     * Gets (as xml) the "PictureURL" element
     */
    org.apache.xmlbeans.XmlString xgetPictureURL();
    
    /**
     * True if has "PictureURL" element
     */
    boolean isSetPictureURL();
    
    /**
     * Sets the "PictureURL" element
     */
    void setPictureURL(java.lang.String pictureURL);
    
    /**
     * Sets (as xml) the "PictureURL" element
     */
    void xsetPictureURL(org.apache.xmlbeans.XmlString pictureURL);
    
    /**
     * Unsets the "PictureURL" element
     */
    void unsetPictureURL();
    
    /**
     * A factory class with static methods for creating instances
     * of this type.
     */
    
    public static final class Factory
    {
        public static com.cdyne.ws.weatherws.WeatherDescription newInstance() {
          return (com.cdyne.ws.weatherws.WeatherDescription) org.apache.xmlbeans.XmlBeans.getContextTypeLoader().newInstance( type, null ); }
        
        public static com.cdyne.ws.weatherws.WeatherDescription newInstance(org.apache.xmlbeans.XmlOptions options) {
          return (com.cdyne.ws.weatherws.WeatherDescription) org.apache.xmlbeans.XmlBeans.getContextTypeLoader().newInstance( type, options ); }
        
        /** @param xmlAsString the string value to parse */
        public static com.cdyne.ws.weatherws.WeatherDescription parse(java.lang.String xmlAsString) throws org.apache.xmlbeans.XmlException {
          return (com.cdyne.ws.weatherws.WeatherDescription) org.apache.xmlbeans.XmlBeans.getContextTypeLoader().parse( xmlAsString, type, null ); }
        
        public static com.cdyne.ws.weatherws.WeatherDescription parse(java.lang.String xmlAsString, org.apache.xmlbeans.XmlOptions options) throws org.apache.xmlbeans.XmlException {
          return (com.cdyne.ws.weatherws.WeatherDescription) org.apache.xmlbeans.XmlBeans.getContextTypeLoader().parse( xmlAsString, type, options ); }
        
        /** @param file the file from which to load an xml document */
        public static com.cdyne.ws.weatherws.WeatherDescription parse(java.io.File file) throws org.apache.xmlbeans.XmlException, java.io.IOException {
          return (com.cdyne.ws.weatherws.WeatherDescription) org.apache.xmlbeans.XmlBeans.getContextTypeLoader().parse( file, type, null ); }
        
        public static com.cdyne.ws.weatherws.WeatherDescription parse(java.io.File file, org.apache.xmlbeans.XmlOptions options) throws org.apache.xmlbeans.XmlException, java.io.IOException {
          return (com.cdyne.ws.weatherws.WeatherDescription) org.apache.xmlbeans.XmlBeans.getContextTypeLoader().parse( file, type, options ); }
        
        public static com.cdyne.ws.weatherws.WeatherDescription parse(java.net.URL u) throws org.apache.xmlbeans.XmlException, java.io.IOException {
          return (com.cdyne.ws.weatherws.WeatherDescription) org.apache.xmlbeans.XmlBeans.getContextTypeLoader().parse( u, type, null ); }
        
        public static com.cdyne.ws.weatherws.WeatherDescription parse(java.net.URL u, org.apache.xmlbeans.XmlOptions options) throws org.apache.xmlbeans.XmlException, java.io.IOException {
          return (com.cdyne.ws.weatherws.WeatherDescription) org.apache.xmlbeans.XmlBeans.getContextTypeLoader().parse( u, type, options ); }
        
        public static com.cdyne.ws.weatherws.WeatherDescription parse(java.io.InputStream is) throws org.apache.xmlbeans.XmlException, java.io.IOException {
          return (com.cdyne.ws.weatherws.WeatherDescription) org.apache.xmlbeans.XmlBeans.getContextTypeLoader().parse( is, type, null ); }
        
        public static com.cdyne.ws.weatherws.WeatherDescription parse(java.io.InputStream is, org.apache.xmlbeans.XmlOptions options) throws org.apache.xmlbeans.XmlException, java.io.IOException {
          return (com.cdyne.ws.weatherws.WeatherDescription) org.apache.xmlbeans.XmlBeans.getContextTypeLoader().parse( is, type, options ); }
        
        public static com.cdyne.ws.weatherws.WeatherDescription parse(java.io.Reader r) throws org.apache.xmlbeans.XmlException, java.io.IOException {
          return (com.cdyne.ws.weatherws.WeatherDescription) org.apache.xmlbeans.XmlBeans.getContextTypeLoader().parse( r, type, null ); }
        
        public static com.cdyne.ws.weatherws.WeatherDescription parse(java.io.Reader r, org.apache.xmlbeans.XmlOptions options) throws org.apache.xmlbeans.XmlException, java.io.IOException {
          return (com.cdyne.ws.weatherws.WeatherDescription) org.apache.xmlbeans.XmlBeans.getContextTypeLoader().parse( r, type, options ); }
        
        public static com.cdyne.ws.weatherws.WeatherDescription parse(javax.xml.stream.XMLStreamReader sr) throws org.apache.xmlbeans.XmlException {
          return (com.cdyne.ws.weatherws.WeatherDescription) org.apache.xmlbeans.XmlBeans.getContextTypeLoader().parse( sr, type, null ); }
        
        public static com.cdyne.ws.weatherws.WeatherDescription parse(javax.xml.stream.XMLStreamReader sr, org.apache.xmlbeans.XmlOptions options) throws org.apache.xmlbeans.XmlException {
          return (com.cdyne.ws.weatherws.WeatherDescription) org.apache.xmlbeans.XmlBeans.getContextTypeLoader().parse( sr, type, options ); }
        
        public static com.cdyne.ws.weatherws.WeatherDescription parse(org.w3c.dom.Node node) throws org.apache.xmlbeans.XmlException {
          return (com.cdyne.ws.weatherws.WeatherDescription) org.apache.xmlbeans.XmlBeans.getContextTypeLoader().parse( node, type, null ); }
        
        public static com.cdyne.ws.weatherws.WeatherDescription parse(org.w3c.dom.Node node, org.apache.xmlbeans.XmlOptions options) throws org.apache.xmlbeans.XmlException {
          return (com.cdyne.ws.weatherws.WeatherDescription) org.apache.xmlbeans.XmlBeans.getContextTypeLoader().parse( node, type, options ); }
        
        /** @deprecated {@link org.apache.xmlbeans.xml.stream.XMLInputStream} */
        public static com.cdyne.ws.weatherws.WeatherDescription parse(org.apache.xmlbeans.xml.stream.XMLInputStream xis) throws org.apache.xmlbeans.XmlException, org.apache.xmlbeans.xml.stream.XMLStreamException {
          return (com.cdyne.ws.weatherws.WeatherDescription) org.apache.xmlbeans.XmlBeans.getContextTypeLoader().parse( xis, type, null ); }
        
        /** @deprecated {@link org.apache.xmlbeans.xml.stream.XMLInputStream} */
        public static com.cdyne.ws.weatherws.WeatherDescription parse(org.apache.xmlbeans.xml.stream.XMLInputStream xis, org.apache.xmlbeans.XmlOptions options) throws org.apache.xmlbeans.XmlException, org.apache.xmlbeans.xml.stream.XMLStreamException {
          return (com.cdyne.ws.weatherws.WeatherDescription) org.apache.xmlbeans.XmlBeans.getContextTypeLoader().parse( xis, type, options ); }
        
        /** @deprecated {@link org.apache.xmlbeans.xml.stream.XMLInputStream} */
        public static org.apache.xmlbeans.xml.stream.XMLInputStream newValidatingXMLInputStream(org.apache.xmlbeans.xml.stream.XMLInputStream xis) throws org.apache.xmlbeans.XmlException, org.apache.xmlbeans.xml.stream.XMLStreamException {
          return org.apache.xmlbeans.XmlBeans.getContextTypeLoader().newValidatingXMLInputStream( xis, type, null ); }
        
        /** @deprecated {@link org.apache.xmlbeans.xml.stream.XMLInputStream} */
        public static org.apache.xmlbeans.xml.stream.XMLInputStream newValidatingXMLInputStream(org.apache.xmlbeans.xml.stream.XMLInputStream xis, org.apache.xmlbeans.XmlOptions options) throws org.apache.xmlbeans.XmlException, org.apache.xmlbeans.xml.stream.XMLStreamException {
          return org.apache.xmlbeans.XmlBeans.getContextTypeLoader().newValidatingXMLInputStream( xis, type, options ); }
        
        private Factory() { } // No instance of this class allowed
    }
}
