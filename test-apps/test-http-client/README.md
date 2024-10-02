This contains tests on jdk 11's HttpClient

### Spring Boot test app with embedded tomcat
1. Make sure you are running JDK 11 on below steps
2. mvn clean package
3. cd target
4. To run embedded spring test app, `java -javaagent:<agent jar location> -jar test-http-client.jar`


### Standalone java app that creates a trace with http client operations
1. Make sure you are running JDK 11 on below steps
2. Disable(comment out) the `spring-boot-maven-plugin` plugin and enable (uncomment) the `maven-assembly-plugin` plugin
3. mvn clean package
4. cd target
5. To run the standalone test app for example, `java -cp test-http-client-jar-with-dependencies.jar -javaagent:<agent jar location> com.appoptics.test.pojo.TestGet`
