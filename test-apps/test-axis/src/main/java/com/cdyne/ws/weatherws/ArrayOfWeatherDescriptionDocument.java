/*
 * An XML document type.
 * Localname: ArrayOfWeatherDescription
 * Namespace: http://ws.cdyne.com/WeatherWS/
 * Java type: com.cdyne.ws.weatherws.ArrayOfWeatherDescriptionDocument
 *
 * Automatically generated - do not modify.
 */
package com.cdyne.ws.weatherws;


/**
 * A document containing one ArrayOfWeatherDescription(@http://ws.cdyne.com/WeatherWS/) element.
 *
 * This is a complex type.
 */
public interface ArrayOfWeatherDescriptionDocument extends org.apache.xmlbeans.XmlObject
{
    public static final org.apache.xmlbeans.SchemaType type = (org.apache.xmlbeans.SchemaType)
        org.apache.xmlbeans.XmlBeans.typeSystemForClassLoader(ArrayOfWeatherDescriptionDocument.class.getClassLoader(), "schemaorg_apache_xmlbeans.system.s9BF98892DFFBF58E7682C993B0C16982").resolveHandle("arrayofweatherdescription486ddoctype");
    
    /**
     * Gets the "ArrayOfWeatherDescription" element
     */
    com.cdyne.ws.weatherws.ArrayOfWeatherDescription getArrayOfWeatherDescription();
    
    /**
     * Tests for nil "ArrayOfWeatherDescription" element
     */
    boolean isNilArrayOfWeatherDescription();
    
    /**
     * Sets the "ArrayOfWeatherDescription" element
     */
    void setArrayOfWeatherDescription(com.cdyne.ws.weatherws.ArrayOfWeatherDescription arrayOfWeatherDescription);
    
    /**
     * Appends and returns a new empty "ArrayOfWeatherDescription" element
     */
    com.cdyne.ws.weatherws.ArrayOfWeatherDescription addNewArrayOfWeatherDescription();
    
    /**
     * Nils the "ArrayOfWeatherDescription" element
     */
    void setNilArrayOfWeatherDescription();
    
    /**
     * A factory class with static methods for creating instances
     * of this type.
     */
    
    public static final class Factory
    {
        public static com.cdyne.ws.weatherws.ArrayOfWeatherDescriptionDocument newInstance() {
          return (com.cdyne.ws.weatherws.ArrayOfWeatherDescriptionDocument) org.apache.xmlbeans.XmlBeans.getContextTypeLoader().newInstance( type, null ); }
        
        public static com.cdyne.ws.weatherws.ArrayOfWeatherDescriptionDocument newInstance(org.apache.xmlbeans.XmlOptions options) {
          return (com.cdyne.ws.weatherws.ArrayOfWeatherDescriptionDocument) org.apache.xmlbeans.XmlBeans.getContextTypeLoader().newInstance( type, options ); }
        
        /** @param xmlAsString the string value to parse */
        public static com.cdyne.ws.weatherws.ArrayOfWeatherDescriptionDocument parse(java.lang.String xmlAsString) throws org.apache.xmlbeans.XmlException {
          return (com.cdyne.ws.weatherws.ArrayOfWeatherDescriptionDocument) org.apache.xmlbeans.XmlBeans.getContextTypeLoader().parse( xmlAsString, type, null ); }
        
        public static com.cdyne.ws.weatherws.ArrayOfWeatherDescriptionDocument parse(java.lang.String xmlAsString, org.apache.xmlbeans.XmlOptions options) throws org.apache.xmlbeans.XmlException {
          return (com.cdyne.ws.weatherws.ArrayOfWeatherDescriptionDocument) org.apache.xmlbeans.XmlBeans.getContextTypeLoader().parse( xmlAsString, type, options ); }
        
        /** @param file the file from which to load an xml document */
        public static com.cdyne.ws.weatherws.ArrayOfWeatherDescriptionDocument parse(java.io.File file) throws org.apache.xmlbeans.XmlException, java.io.IOException {
          return (com.cdyne.ws.weatherws.ArrayOfWeatherDescriptionDocument) org.apache.xmlbeans.XmlBeans.getContextTypeLoader().parse( file, type, null ); }
        
        public static com.cdyne.ws.weatherws.ArrayOfWeatherDescriptionDocument parse(java.io.File file, org.apache.xmlbeans.XmlOptions options) throws org.apache.xmlbeans.XmlException, java.io.IOException {
          return (com.cdyne.ws.weatherws.ArrayOfWeatherDescriptionDocument) org.apache.xmlbeans.XmlBeans.getContextTypeLoader().parse( file, type, options ); }
        
        public static com.cdyne.ws.weatherws.ArrayOfWeatherDescriptionDocument parse(java.net.URL u) throws org.apache.xmlbeans.XmlException, java.io.IOException {
          return (com.cdyne.ws.weatherws.ArrayOfWeatherDescriptionDocument) org.apache.xmlbeans.XmlBeans.getContextTypeLoader().parse( u, type, null ); }
        
        public static com.cdyne.ws.weatherws.ArrayOfWeatherDescriptionDocument parse(java.net.URL u, org.apache.xmlbeans.XmlOptions options) throws org.apache.xmlbeans.XmlException, java.io.IOException {
          return (com.cdyne.ws.weatherws.ArrayOfWeatherDescriptionDocument) org.apache.xmlbeans.XmlBeans.getContextTypeLoader().parse( u, type, options ); }
        
        public static com.cdyne.ws.weatherws.ArrayOfWeatherDescriptionDocument parse(java.io.InputStream is) throws org.apache.xmlbeans.XmlException, java.io.IOException {
          return (com.cdyne.ws.weatherws.ArrayOfWeatherDescriptionDocument) org.apache.xmlbeans.XmlBeans.getContextTypeLoader().parse( is, type, null ); }
        
        public static com.cdyne.ws.weatherws.ArrayOfWeatherDescriptionDocument parse(java.io.InputStream is, org.apache.xmlbeans.XmlOptions options) throws org.apache.xmlbeans.XmlException, java.io.IOException {
          return (com.cdyne.ws.weatherws.ArrayOfWeatherDescriptionDocument) org.apache.xmlbeans.XmlBeans.getContextTypeLoader().parse( is, type, options ); }
        
        public static com.cdyne.ws.weatherws.ArrayOfWeatherDescriptionDocument parse(java.io.Reader r) throws org.apache.xmlbeans.XmlException, java.io.IOException {
          return (com.cdyne.ws.weatherws.ArrayOfWeatherDescriptionDocument) org.apache.xmlbeans.XmlBeans.getContextTypeLoader().parse( r, type, null ); }
        
        public static com.cdyne.ws.weatherws.ArrayOfWeatherDescriptionDocument parse(java.io.Reader r, org.apache.xmlbeans.XmlOptions options) throws org.apache.xmlbeans.XmlException, java.io.IOException {
          return (com.cdyne.ws.weatherws.ArrayOfWeatherDescriptionDocument) org.apache.xmlbeans.XmlBeans.getContextTypeLoader().parse( r, type, options ); }
        
        public static com.cdyne.ws.weatherws.ArrayOfWeatherDescriptionDocument parse(javax.xml.stream.XMLStreamReader sr) throws org.apache.xmlbeans.XmlException {
          return (com.cdyne.ws.weatherws.ArrayOfWeatherDescriptionDocument) org.apache.xmlbeans.XmlBeans.getContextTypeLoader().parse( sr, type, null ); }
        
        public static com.cdyne.ws.weatherws.ArrayOfWeatherDescriptionDocument parse(javax.xml.stream.XMLStreamReader sr, org.apache.xmlbeans.XmlOptions options) throws org.apache.xmlbeans.XmlException {
          return (com.cdyne.ws.weatherws.ArrayOfWeatherDescriptionDocument) org.apache.xmlbeans.XmlBeans.getContextTypeLoader().parse( sr, type, options ); }
        
        public static com.cdyne.ws.weatherws.ArrayOfWeatherDescriptionDocument parse(org.w3c.dom.Node node) throws org.apache.xmlbeans.XmlException {
          return (com.cdyne.ws.weatherws.ArrayOfWeatherDescriptionDocument) org.apache.xmlbeans.XmlBeans.getContextTypeLoader().parse( node, type, null ); }
        
        public static com.cdyne.ws.weatherws.ArrayOfWeatherDescriptionDocument parse(org.w3c.dom.Node node, org.apache.xmlbeans.XmlOptions options) throws org.apache.xmlbeans.XmlException {
          return (com.cdyne.ws.weatherws.ArrayOfWeatherDescriptionDocument) org.apache.xmlbeans.XmlBeans.getContextTypeLoader().parse( node, type, options ); }
        
        /** @deprecated {@link org.apache.xmlbeans.xml.stream.XMLInputStream} */
        public static com.cdyne.ws.weatherws.ArrayOfWeatherDescriptionDocument parse(org.apache.xmlbeans.xml.stream.XMLInputStream xis) throws org.apache.xmlbeans.XmlException, org.apache.xmlbeans.xml.stream.XMLStreamException {
          return (com.cdyne.ws.weatherws.ArrayOfWeatherDescriptionDocument) org.apache.xmlbeans.XmlBeans.getContextTypeLoader().parse( xis, type, null ); }
        
        /** @deprecated {@link org.apache.xmlbeans.xml.stream.XMLInputStream} */
        public static com.cdyne.ws.weatherws.ArrayOfWeatherDescriptionDocument parse(org.apache.xmlbeans.xml.stream.XMLInputStream xis, org.apache.xmlbeans.XmlOptions options) throws org.apache.xmlbeans.XmlException, org.apache.xmlbeans.xml.stream.XMLStreamException {
          return (com.cdyne.ws.weatherws.ArrayOfWeatherDescriptionDocument) org.apache.xmlbeans.XmlBeans.getContextTypeLoader().parse( xis, type, options ); }
        
        /** @deprecated {@link org.apache.xmlbeans.xml.stream.XMLInputStream} */
        public static org.apache.xmlbeans.xml.stream.XMLInputStream newValidatingXMLInputStream(org.apache.xmlbeans.xml.stream.XMLInputStream xis) throws org.apache.xmlbeans.XmlException, org.apache.xmlbeans.xml.stream.XMLStreamException {
          return org.apache.xmlbeans.XmlBeans.getContextTypeLoader().newValidatingXMLInputStream( xis, type, null ); }
        
        /** @deprecated {@link org.apache.xmlbeans.xml.stream.XMLInputStream} */
        public static org.apache.xmlbeans.xml.stream.XMLInputStream newValidatingXMLInputStream(org.apache.xmlbeans.xml.stream.XMLInputStream xis, org.apache.xmlbeans.XmlOptions options) throws org.apache.xmlbeans.XmlException, org.apache.xmlbeans.xml.stream.XMLStreamException {
          return org.apache.xmlbeans.XmlBeans.getContextTypeLoader().newValidatingXMLInputStream( xis, type, options ); }
        
        private Factory() { } // No instance of this class allowed
    }
}
