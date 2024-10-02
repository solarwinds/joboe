Sample Glassfish Jersey REST server running on min support version 2.0 

## Deployment
Either build the project with maven:
```
mvn clean package
```
and deploy the built war into an application server (such as tomcat)

or run as a standalone by running `com.appoptics.test.Standalone` directly

RESTful urls:
GET /users
GET /users/{username}
PUT /users/{username}
DELETE /users/{username}
GET /users/{username}/messages
PUT /users/{username}/messages
GET /users/{username}/messages/{messageId}
DELETE  /users/{username}/messages/{messageId}
