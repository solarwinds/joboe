Sample IBM websphere project for OSGi created from  
http://www.themiddlewareshop.com/2012/11/11/osgi-websphere-sample-exercise/

You must have installed IBM Websphere Application Server before deploying this app:
http://www14.software.ibm.com/webapp/download/search.jsp?pn=WebSphere+Application+Server


To deploy this OSGi testing webapp, there are 2 files
-OSGICalculatorApp_1.0.0.eba (Business level application archive)
-OSGICalculatorImpl_1.0.0.jar (OSGi bundle)

In the first link above:
-skip to the step "Deploy a bundle using WAS console", deploy the OSGICalculatorImpl_1.0.0.jar as described
-skip to the step "Deploy Assets", deploy the OSGICalculatorApp_1.0.0.eba as described
-continue to rest of the steps to "Start the Application"

The app should be available at http://localhost:9081/OSGICalculatorServlet/OSGICalculatorServlet , it should display something like "Reuslt = 25"

To instrument websphere server, you would have to include our java agent in the websphere process:

1. Start the Websphere Application Server Admin console
2. Click on left panel: Servers => Server Types => WebSphere application Servers
3. Click the link of the Websphere application Servers you wish to include java agent in
4. In the Configuration tab, expand Java and Process Management under the Server Infrastructure section
5. Click Process definition
6. Click on Java Virtual Machine under Additional Properties section
7. In the Configuration tab, locate the Generic JVM arguments field, append the value with the javaagent arugment 
Windows: 
-javaagent:(ProgramFiles)\AppNeta\TraceView\java\tracelyticsagent.jar
Linux:
-javaagent:/usr/local/tracelytics/tracelyticsagent.jar
8. Apply and Save the changes
9. Restart the server and the requests should be traced now
 


