Test case for gRPC 1.26.0

For normal server/client communications, run RouteGuideServer and RouteGuideClient on separate terminals. Enable javaagent by adding -javaagent:"<agent installation path>\appoptics-agent.jar" to the java run command
For asynchronous client calls, run RouteGuideServer and TestAsyncClient on separate terminals.
For server exception, Run ErrorHandlingClient on a single terminal
For client exception, Run RouteGuideServer and TestErrorClient on separate terminals.

For more information, refer to https://github.com/grpc/grpc-java/tree/v1.26.0/examples

To update test cases: 
- check out the latest tag in https://github.com/grpc/grpc-java
- navigate to ~/git/grpc-java/examples
- execute `./gradlew installDist`
- copy the generated source from `grpc-java\examples\build\generated\source\proto\main\java\io` and `grpc-java\examples\build\generated\source\proto\main\grpc\io` to this project 
- add back the code :
```
AgentChecker.waitUntilAgentReady(5, TimeUnit.SECONDS);
Trace.startTrace("test-grpc-client").report();
...
Trace.endTrace("test-grpc-client")
Above might not be the best way to do it. Perhaps we should make a clone of the gRPC example project directly 


To test gRPC with nginx as the load balancer and reversed proxy, try with the nginx.conf under this directory.
Note that you may change the ports of the gRPC server/client accordingly.
1. Modify the pom.xml to build the gRPC RouteGuide client jar
2. Modify the gRPC server port to 8080 in RouteGuideServer.java
3. start up nginx with the nginx.conf under this directory.
4. run the client jar to connect to the server via nginx.