A testing app for Spring boot + Freemarker as template framework

Package the project as jar
`mvn package`

Execute the jar with javaagent such as
`java -javaagent:/usr/local/appoptics/appoptics-agent.jar -jar test-spring-boot-freemarker-2.1.0.RELEASE.jar`


Somehow this does NOT work on my Windows PC (could not resolve the Freemarker view) yet it works fine on Linux (EC2 ubuntu instance) 