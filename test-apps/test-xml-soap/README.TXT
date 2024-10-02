This is an testing application for javax.xml.soap client instrumentation


Links below would trigger web-service call to below:
http://<host>/test-xml-soap/weather.do -weather forecast API (SOAP)
http://<host>/test-xml-soap/simple.do -sample local soap server (SOAP, see test-apps/test-soap-server)

In order to test on different versions, deploy on application server that runs on different JVM (tested jdk 1.6 - 10 latest)

In order to test on jdk 1.5 , please deploy test-xml-soap-min instead
