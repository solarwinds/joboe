Servlet that runs on Tomcat and calls into an EJB3 running on JBoss.

Refer to test-ejb3ear for deploying EJB3 service on JBoss

Then deploy this webapp to Tomcat (JBoss ear should take 8081) and access thru:

http://localvm:8080/testejb3-webapp/test

Take note that this one only works up to jboss 5.1.0 for jboss 6+ please use test-ejb3client-jboss6 and est-ejb3client-jboss7
