Testing endpoint for axis SOAP service. (latest axis2 version 1.7.9)

This is only tested by deploying on Eclipse IDE on Tomcat 7 (JDK 7). Not yet sure how to deploy it as a regular webapp.

From eclipse, deploy this as a web application to tomcat 7 instance

After starting, the wsdl should be available at:
http://localhost:8080/test-axis-service/services/TestService?wsdl


The testing SOAP client is also included in this project, access endpoint:
http://localhost:61613/wse/wsexplorer.jsp

Click the WSDL page button on top right, then enter the WSDL definition
http://localhost:8080/test-axis-service/services/TestService?wsdl

It should then load up the SOAP actions available, click through each of the action for testing

