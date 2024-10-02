To build:
```
mvn clean package
```

To Run:
java -javaagent:<agent location> -jar target/test-spring-boot-reactive-redisson-0.0.1-SNAPSHOT.jar

This by default starts an embedded tomcat with testing endpoint:
http://localhost:8080/hello


Other http server can also be "switched in":
1. Uncomment the lines in pom.xml of <exclusions> under <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-web</artifactId>...
2. Uncomment the http server to use : spring-boot-starter-undertow or spring-boot-starter-jetty
3. Rebuild and restart the application