Test case for gRPC 1.0.0

For normal server/client communications, run RouteGuideServer and RouteGuideClient on separate terminals. Enable javaagent by adding -javaagent:"<agent installation path>\appoptics-agent.jar" to the java run command
For asynchronous client calls, run RouteGuideServer and TestAsyncClient on separate terminals.
For server exception, Run ErrorHandlingClient on a single terminal
For client exception, Run RouteGuideServer and TestErrorClient on separate terminals.

For more information, refer to https://github.com/grpc/grpc-java/tree/v1.0.0/examples
