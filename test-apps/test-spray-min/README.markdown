## _spray_ test project on spray 1.1.3

This project contains 2 main sub-projects:
1. spray-can standalone server + spray-routing
2. spray-can standalone server only


### Option 1 : Run the packaged jar directly

Executable jars are generated and placed in the "assembly" folder. The jars can be run directly such as
java -javaagent:"/usr/local/tracelytics/tracelyticsagent.jar" -jar test-spray-can-and-routing.jar

### Option 2: Build and start the project from source

either sbt or activator has to be installed on the system

1. Build the Spray 1.1.3, it's not available publicly anymore. Clone repo https://github.com/spray/spray, switch to branch "release/1.1"
2. Deploy the spray to local ivy repo, use sbt publish-local
3. To run the project, navigate to the root directory of the project (ie /home/my-user/git/joboe/test-apps/test-spray-min)
4. Configure sbt/activator to run our agent. 
For sbt:
 goto the <sbt installation folder>/conf
 modify sbtconfig.txt and add line
 -javaagent:"/usr/local/tracelytics/tracelyticsagent.jar"
For activitor:
 goto the <user home foler>/.activator
 create or modify activatorconfig.txt and add line
 -javaagent:"/usr/local/tracelytics/tracelyticsagent.jar"
 
 
OR set environment variable SBT_OPTS or JAVA_OPTS with the javaagent option
 
5. run command below
   sbt run
   OR
   activator run
6. This should start the update process and it will eventually prompt:

Multiple main classes detected, select one to run:

 [1] com.example.Boot
 [2] http.TestHttpServer
 
7. com.example.Boot is the spray-can standalone server + spray-routing; while http.TestTHttpServer is the plain spray-can standalone testing app
8. for [1], access http://localhost:8080, it will prompt a list of possible actions
9. for [2], acesss http://localhost:8080/ping for simple ping response. Use any other links to test timeout


   