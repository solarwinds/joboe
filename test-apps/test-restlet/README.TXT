This is an testing application for Restlet REST client (latest version 2.3)

Take note that this uses the Apache Http extention as the Http client, to disable it (use default HttpUrlConnnection instead) simply comment out in the pom.xml: 
		<dependency>
			<groupId>org.restlet.jse</groupId>
			<artifactId>org.restlet.ext.httpclient</artifactId>
			<version>2.3-M5</version>
		</dependency>

and rebuild the project  

Links below would trigger web-service call to below:
http://<host>/test-restlet/simple.do -sample local REST server (REST, see test-apps/test-rest-server)
