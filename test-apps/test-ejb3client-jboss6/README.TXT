Servlet that runs on Tomcat and calls into an EJB3 running on JBoss.

Refer to test-ejb3ear for deploying EJB3 service on JBoss

Then deploy this webapp to Tomcat (JBoss ear should take 8081) and access thru:

http://localvm:8080/testejb3-webapp-jboss6/test

Take note that this one only work for jboss 6, for jboss 7+ please use test-ejb3client-jboss7

if encountered
avax.naming.AuthenticationException: Failed to login using protocol=other [Root exception is javax.security.auth.login.LoginException: No LoginModules configured for other]...

Make sure the client side server is using jdk 6, using jdk 7 seems to trigger that problem