A simple hello world grizzly server that runs servlet on Grizzly version 2.4.4

Simple run the Standalone class (if imported to IDE)

Otherwise build with maven:
```
mvn clean package
```
For standalone (blocking)

```
java -javaagent:<agent path> -jar target/test-glassfish-grizzly-jar-with-dependencies.jar
```
For standalone (non-block)

```
java -javaagent:<agent path> -cp target/test-glassfish-grizzly-jar-with-dependencies.jar com.appoptics.test.StandaloneNonBlocking
```

Test on endpoint http://localhost:8080/something

For non-blcoking requests, it would take 10 seconds for the response, to test timeout add `timeout` parameter for example

http://localhost:8080/something?timeout=1000

would have timeout of 1 sec
