The endpoint for the Spring Webflux on Jetty test: 
http://localhost:8080/hello
curl -i http://localhost:8080/name/{name}   # {name} can be replaced by any string.

Note that if you want to test Webflux on Tomcat/Undertow, just go to the pom.xml file and replace
the Jetty dependency with the commented tomcat/undertow one.

See the tutorial at https://spring.io/guides/gs/reactive-rest-service/
