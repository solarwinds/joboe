This is a testing application for Jboss RESTEasy REST client instrumentation (packaged with JBoss7, 2.3.1.GA)

JBoss 7 come packaged with an earlier version of RESTEasy, which doesn't support some of the functions called 
in the "max" version of our test-resteasy application. Moreover, the resteasy test-apps are standalone, which
might not match the use case of people using the prepackaged version. So this test-app uses the resteasy packages
that are provided with JBoss 7. 

Links below would trigger web-service call to below:
http://<host>/test-resteasy-jboss7/simple.do -TraceView public API (REST)
http://<host>/test-resteasy-jboss7/simple.do -sample local REST server (REST, see test-apps/test-rest-server)
