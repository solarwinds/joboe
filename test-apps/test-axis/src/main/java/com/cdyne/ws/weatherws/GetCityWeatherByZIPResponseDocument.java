/*
 * An XML document type.
 * Localname: GetCityWeatherByZIPResponse
 * Namespace: http://ws.cdyne.com/WeatherWS/
 * Java type: com.cdyne.ws.weatherws.GetCityWeatherByZIPResponseDocument
 *
 * Automatically generated - do not modify.
 */
package com.cdyne.ws.weatherws;


/**
 * A document containing one GetCityWeatherByZIPResponse(@http://ws.cdyne.com/WeatherWS/) element.
 *
 * This is a complex type.
 */
public interface GetCityWeatherByZIPResponseDocument extends org.apache.xmlbeans.XmlObject
{
    public static final org.apache.xmlbeans.SchemaType type = (org.apache.xmlbeans.SchemaType)
        org.apache.xmlbeans.XmlBeans.typeSystemForClassLoader(GetCityWeatherByZIPResponseDocument.class.getClassLoader(), "schemaorg_apache_xmlbeans.system.s9BF98892DFFBF58E7682C993B0C16982").resolveHandle("getcityweatherbyzipresponseb22ddoctype");
    
    /**
     * Gets the "GetCityWeatherByZIPResponse" element
     */
    com.cdyne.ws.weatherws.GetCityWeatherByZIPResponseDocument.GetCityWeatherByZIPResponse getGetCityWeatherByZIPResponse();
    
    /**
     * Sets the "GetCityWeatherByZIPResponse" element
     */
    void setGetCityWeatherByZIPResponse(com.cdyne.ws.weatherws.GetCityWeatherByZIPResponseDocument.GetCityWeatherByZIPResponse getCityWeatherByZIPResponse);
    
    /**
     * Appends and returns a new empty "GetCityWeatherByZIPResponse" element
     */
    com.cdyne.ws.weatherws.GetCityWeatherByZIPResponseDocument.GetCityWeatherByZIPResponse addNewGetCityWeatherByZIPResponse();
    
    /**
     * An XML GetCityWeatherByZIPResponse(@http://ws.cdyne.com/WeatherWS/).
     *
     * This is a complex type.
     */
    public interface GetCityWeatherByZIPResponse extends org.apache.xmlbeans.XmlObject
    {
        public static final org.apache.xmlbeans.SchemaType type = (org.apache.xmlbeans.SchemaType)
            org.apache.xmlbeans.XmlBeans.typeSystemForClassLoader(GetCityWeatherByZIPResponse.class.getClassLoader(), "schemaorg_apache_xmlbeans.system.s9BF98892DFFBF58E7682C993B0C16982").resolveHandle("getcityweatherbyzipresponse92c9elemtype");
        
        /**
         * Gets the "GetCityWeatherByZIPResult" element
         */
        com.cdyne.ws.weatherws.WeatherReturn getGetCityWeatherByZIPResult();
        
        /**
         * Sets the "GetCityWeatherByZIPResult" element
         */
        void setGetCityWeatherByZIPResult(com.cdyne.ws.weatherws.WeatherReturn getCityWeatherByZIPResult);
        
        /**
         * Appends and returns a new empty "GetCityWeatherByZIPResult" element
         */
        com.cdyne.ws.weatherws.WeatherReturn addNewGetCityWeatherByZIPResult();
        
        /**
         * A factory class with static methods for creating instances
         * of this type.
         */
        
        public static final class Factory
        {
            public static com.cdyne.ws.weatherws.GetCityWeatherByZIPResponseDocument.GetCityWeatherByZIPResponse newInstance() {
              return (com.cdyne.ws.weatherws.GetCityWeatherByZIPResponseDocument.GetCityWeatherByZIPResponse) org.apache.xmlbeans.XmlBeans.getContextTypeLoader().newInstance( type, null ); }
            
            public static com.cdyne.ws.weatherws.GetCityWeatherByZIPResponseDocument.GetCityWeatherByZIPResponse newInstance(org.apache.xmlbeans.XmlOptions options) {
              return (com.cdyne.ws.weatherws.GetCityWeatherByZIPResponseDocument.GetCityWeatherByZIPResponse) org.apache.xmlbeans.XmlBeans.getContextTypeLoader().newInstance( type, options ); }
            
            private Factory() { } // No instance of this class allowed
        }
    }
    
    /**
     * A factory class with static methods for creating instances
     * of this type.
     */
    
    public static final class Factory
    {
        public static com.cdyne.ws.weatherws.GetCityWeatherByZIPResponseDocument newInstance() {
          return (com.cdyne.ws.weatherws.GetCityWeatherByZIPResponseDocument) org.apache.xmlbeans.XmlBeans.getContextTypeLoader().newInstance( type, null ); }
        
        public static com.cdyne.ws.weatherws.GetCityWeatherByZIPResponseDocument newInstance(org.apache.xmlbeans.XmlOptions options) {
          return (com.cdyne.ws.weatherws.GetCityWeatherByZIPResponseDocument) org.apache.xmlbeans.XmlBeans.getContextTypeLoader().newInstance( type, options ); }
        
        /** @param xmlAsString the string value to parse */
        public static com.cdyne.ws.weatherws.GetCityWeatherByZIPResponseDocument parse(java.lang.String xmlAsString) throws org.apache.xmlbeans.XmlException {
          return (com.cdyne.ws.weatherws.GetCityWeatherByZIPResponseDocument) org.apache.xmlbeans.XmlBeans.getContextTypeLoader().parse( xmlAsString, type, null ); }
        
        public static com.cdyne.ws.weatherws.GetCityWeatherByZIPResponseDocument parse(java.lang.String xmlAsString, org.apache.xmlbeans.XmlOptions options) throws org.apache.xmlbeans.XmlException {
          return (com.cdyne.ws.weatherws.GetCityWeatherByZIPResponseDocument) org.apache.xmlbeans.XmlBeans.getContextTypeLoader().parse( xmlAsString, type, options ); }
        
        /** @param file the file from which to load an xml document */
        public static com.cdyne.ws.weatherws.GetCityWeatherByZIPResponseDocument parse(java.io.File file) throws org.apache.xmlbeans.XmlException, java.io.IOException {
          return (com.cdyne.ws.weatherws.GetCityWeatherByZIPResponseDocument) org.apache.xmlbeans.XmlBeans.getContextTypeLoader().parse( file, type, null ); }
        
        public static com.cdyne.ws.weatherws.GetCityWeatherByZIPResponseDocument parse(java.io.File file, org.apache.xmlbeans.XmlOptions options) throws org.apache.xmlbeans.XmlException, java.io.IOException {
          return (com.cdyne.ws.weatherws.GetCityWeatherByZIPResponseDocument) org.apache.xmlbeans.XmlBeans.getContextTypeLoader().parse( file, type, options ); }
        
        public static com.cdyne.ws.weatherws.GetCityWeatherByZIPResponseDocument parse(java.net.URL u) throws org.apache.xmlbeans.XmlException, java.io.IOException {
          return (com.cdyne.ws.weatherws.GetCityWeatherByZIPResponseDocument) org.apache.xmlbeans.XmlBeans.getContextTypeLoader().parse( u, type, null ); }
        
        public static com.cdyne.ws.weatherws.GetCityWeatherByZIPResponseDocument parse(java.net.URL u, org.apache.xmlbeans.XmlOptions options) throws org.apache.xmlbeans.XmlException, java.io.IOException {
          return (com.cdyne.ws.weatherws.GetCityWeatherByZIPResponseDocument) org.apache.xmlbeans.XmlBeans.getContextTypeLoader().parse( u, type, options ); }
        
        public static com.cdyne.ws.weatherws.GetCityWeatherByZIPResponseDocument parse(java.io.InputStream is) throws org.apache.xmlbeans.XmlException, java.io.IOException {
          return (com.cdyne.ws.weatherws.GetCityWeatherByZIPResponseDocument) org.apache.xmlbeans.XmlBeans.getContextTypeLoader().parse( is, type, null ); }
        
        public static com.cdyne.ws.weatherws.GetCityWeatherByZIPResponseDocument parse(java.io.InputStream is, org.apache.xmlbeans.XmlOptions options) throws org.apache.xmlbeans.XmlException, java.io.IOException {
          return (com.cdyne.ws.weatherws.GetCityWeatherByZIPResponseDocument) org.apache.xmlbeans.XmlBeans.getContextTypeLoader().parse( is, type, options ); }
        
        public static com.cdyne.ws.weatherws.GetCityWeatherByZIPResponseDocument parse(java.io.Reader r) throws org.apache.xmlbeans.XmlException, java.io.IOException {
          return (com.cdyne.ws.weatherws.GetCityWeatherByZIPResponseDocument) org.apache.xmlbeans.XmlBeans.getContextTypeLoader().parse( r, type, null ); }
        
        public static com.cdyne.ws.weatherws.GetCityWeatherByZIPResponseDocument parse(java.io.Reader r, org.apache.xmlbeans.XmlOptions options) throws org.apache.xmlbeans.XmlException, java.io.IOException {
          return (com.cdyne.ws.weatherws.GetCityWeatherByZIPResponseDocument) org.apache.xmlbeans.XmlBeans.getContextTypeLoader().parse( r, type, options ); }
        
        public static com.cdyne.ws.weatherws.GetCityWeatherByZIPResponseDocument parse(javax.xml.stream.XMLStreamReader sr) throws org.apache.xmlbeans.XmlException {
          return (com.cdyne.ws.weatherws.GetCityWeatherByZIPResponseDocument) org.apache.xmlbeans.XmlBeans.getContextTypeLoader().parse( sr, type, null ); }
        
        public static com.cdyne.ws.weatherws.GetCityWeatherByZIPResponseDocument parse(javax.xml.stream.XMLStreamReader sr, org.apache.xmlbeans.XmlOptions options) throws org.apache.xmlbeans.XmlException {
          return (com.cdyne.ws.weatherws.GetCityWeatherByZIPResponseDocument) org.apache.xmlbeans.XmlBeans.getContextTypeLoader().parse( sr, type, options ); }
        
        public static com.cdyne.ws.weatherws.GetCityWeatherByZIPResponseDocument parse(org.w3c.dom.Node node) throws org.apache.xmlbeans.XmlException {
          return (com.cdyne.ws.weatherws.GetCityWeatherByZIPResponseDocument) org.apache.xmlbeans.XmlBeans.getContextTypeLoader().parse( node, type, null ); }
        
        public static com.cdyne.ws.weatherws.GetCityWeatherByZIPResponseDocument parse(org.w3c.dom.Node node, org.apache.xmlbeans.XmlOptions options) throws org.apache.xmlbeans.XmlException {
          return (com.cdyne.ws.weatherws.GetCityWeatherByZIPResponseDocument) org.apache.xmlbeans.XmlBeans.getContextTypeLoader().parse( node, type, options ); }
        
        /** @deprecated {@link org.apache.xmlbeans.xml.stream.XMLInputStream} */
        public static com.cdyne.ws.weatherws.GetCityWeatherByZIPResponseDocument parse(org.apache.xmlbeans.xml.stream.XMLInputStream xis) throws org.apache.xmlbeans.XmlException, org.apache.xmlbeans.xml.stream.XMLStreamException {
          return (com.cdyne.ws.weatherws.GetCityWeatherByZIPResponseDocument) org.apache.xmlbeans.XmlBeans.getContextTypeLoader().parse( xis, type, null ); }
        
        /** @deprecated {@link org.apache.xmlbeans.xml.stream.XMLInputStream} */
        public static com.cdyne.ws.weatherws.GetCityWeatherByZIPResponseDocument parse(org.apache.xmlbeans.xml.stream.XMLInputStream xis, org.apache.xmlbeans.XmlOptions options) throws org.apache.xmlbeans.XmlException, org.apache.xmlbeans.xml.stream.XMLStreamException {
          return (com.cdyne.ws.weatherws.GetCityWeatherByZIPResponseDocument) org.apache.xmlbeans.XmlBeans.getContextTypeLoader().parse( xis, type, options ); }
        
        /** @deprecated {@link org.apache.xmlbeans.xml.stream.XMLInputStream} */
        public static org.apache.xmlbeans.xml.stream.XMLInputStream newValidatingXMLInputStream(org.apache.xmlbeans.xml.stream.XMLInputStream xis) throws org.apache.xmlbeans.XmlException, org.apache.xmlbeans.xml.stream.XMLStreamException {
          return org.apache.xmlbeans.XmlBeans.getContextTypeLoader().newValidatingXMLInputStream( xis, type, null ); }
        
        /** @deprecated {@link org.apache.xmlbeans.xml.stream.XMLInputStream} */
        public static org.apache.xmlbeans.xml.stream.XMLInputStream newValidatingXMLInputStream(org.apache.xmlbeans.xml.stream.XMLInputStream xis, org.apache.xmlbeans.XmlOptions options) throws org.apache.xmlbeans.XmlException, org.apache.xmlbeans.xml.stream.XMLStreamException {
          return org.apache.xmlbeans.XmlBeans.getContextTypeLoader().newValidatingXMLInputStream( xis, type, options ); }
        
        private Factory() { } // No instance of this class allowed
    }
}
