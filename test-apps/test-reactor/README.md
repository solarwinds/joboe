To build:
```
mvn clean package
```

To Run:
java -javaagent:<agent location> -jar target/test-reactive-0.0.1-SNAPSHOT.jar

This by default starts an embedded tomcat with testing endpoint:
http://localhost:8080/hello

This end point triggers 3 concurrent fluxes each with some pause and custom SDK spans

