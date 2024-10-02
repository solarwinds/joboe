This is a small test application for TraceView API calls (built on a modified test-struts project).

You can run it in your IDE, or from the command line.

From your IDE: depends on the IDE.  I use run-jetty-run for Eclipse: http://code.google.com/p/run-jetty-run/.

Command line:
(1) Open a terminal or console and navigate to the directory where pom.xml lives.
  
(2) Run "mvn package" in this directory (where pom.xml lives), and you'll a WAR file you can stick in Tomcat or Jetty.

(3) Standard Tracelytics javaagent stuff from there.

Either way, the app is accessible by default at 
http://localhost:8080/test-api/test (this tests the new API in tracelytics-api.jar)
http://localhost:8080/test-api/return (test on serialization of various return values using our annotations)
http://localhost:8080/test-api/max-event?eventCount=123
http://localhost:8080/test-api/custom-transaction-name?customTransactionName=my%20name%20here
