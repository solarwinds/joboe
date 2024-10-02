This is an testing application for Restlet REST client (MIN version 2.0)

Take note that this uses the default (Socket) for http requests to use Apache http client instead, UNCOMMENT in pom.xml 
<!-- 		<dependency> -->
<!-- 			<groupId>org.restlet.jse</groupId> -->
<!-- 			<artifactId>org.restlet.ext.httpclient</artifactId> -->
<!-- 			<version>2.0.0</version> -->
<!-- 		</dependency> -->

and rebuild the project  

Links below would trigger web-service call to below:
http://<host>/test-restlet-min/simple.do -sample local REST server (REST, see test-apps/test-rest-server)
