Sample Glassfish Jersey REST server running on latest support version 2.29.1


## Deployment
Either build the project with maven:
```
mvn clean package
```
and deploy the built war into an application server (such as tomcat)

or run as a standalone by running `com.appoptics.test.Standalone` directly


## Endpoints
RESTful urls:
GET http://localhost:8080/test-glassfish-jersey-server/rest/users
GET http://localhost:8080/test-glassfish-jersey-server/rest/users/{username}
PUT http://localhost:8080/test-glassfish-jersey-server/rest/users/{username}
DELETE http://localhost:8080/test-glassfish-jersey-server/rest/users/{username}
GET http://localhost:8080/test-glassfish-jersey-server/rest/users/{username}/messages
PUT http://localhost:8080/test-glassfish-jersey-server/rest/users/{username}/messages
GET http://localhost:8080/test-glassfish-jersey-server/rest/users/{username}/messages/{messageId}
DELETE http://localhost:8080/test-glassfish-jersey-server/rest/users/{username}/messages/{messageId}
