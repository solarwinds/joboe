## _spray_ test project on spray 1.3.3

This project contains 2 main sub-projects:
1. spray-can standalone server + spray-routing
2. spray-can standalone server only

### Option 1 : Run the packaged jar directly

Executable jars are generated and placed in the "assembly" folder. The jars can be run directly such as
java -javaagent:"/usr/local/tracelytics/tracelyticsagent.jar" -jar test-spray-can-and-routing.jar

To run the spray with kamon simply run the test-spray-kamon.jar along with the -jar of aspectj-weaver (which kamon relies on, also included in the assembly folder). Therefore to run Kamon with our agent, it would be
java -javaagent:aspectjweaver-1.8.7.jar -javaagent:"/usr/local/tracelytics/tracelyticsagent.jar" -jar test-spray-kamon.jar


### Option 2 : Build and start the project from source

either sbt or activator has to be installed on the system

1. To run the project, navigate to the root directory of the project (ie /home/my-user/git/joboe/test-apps/test-spray)
2. Configure sbt/activator to run our agent. 
For sbt:
 goto the <sbt installation folder>/conf
 modify sbtconfig.txt and add line
 -javaagent:"/usr/local/tracelytics/tracelyticsagent.jar"
For activitor:
 goto the <user home foler>/.activator
 create or modify activatorconfig.txt and add line
 -javaagent:"/usr/local/tracelytics/tracelyticsagent.jar"
 
 
OR set environment variable SBT_OPTS or JAVA_OPTS with the javaagent option
 
3. run command below
   sbt run
   OR
   activator run
4. This should start the update process and it will eventually prompt:

Multiple main classes detected, select one to run:

 [1] com.example.Boot
 [2] http.TestHttpServer
 
5. com.example.Boot is the spray-can standalone server + spray-routing; while http.TestTHttpServer is the plain spray-can standalone testing app
6. for [1], access http://localhost:8080, it will prompt a list of possible actions
7. for [2], acesss http://localhost:8080/ping for simple ping response. Use any other links to test timeout

Take note that this project has Kamon monitoring integrated. To run with Kamon, simply:
1. Execute activator or sbt
2. Within the activator/sbt console, type 
 aspectj-runner:run 

To disable all Kamon:
1. comment out code from Gloabl.scala (or just remove the class)
2. comment out the Kamon plugin references in the build.sbt
3. comment out the Kamon plugin reference in project/plugins.sbt

To disable the Kamon module that uses aspectJ (that might conflict with our agent)
1. Within the activator/sbt console, instead of "aspectj-runner:run", simply do
 run
2. You will see a big warning saying the AspectJ Weaver is not running. This is expected



To build the Spray jar, refer to https://github.com/sbt/sbt-assembly:
1. change in build.sbt the main class of the assembly
2. run 
sbt
3. Within sbt:
assembly


   