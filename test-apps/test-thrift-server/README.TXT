This testing project contains 3 pairs of Thrift test cases:

1. Plain standalone Thrift server and client
 - Run ThriftPlainServer as a standalone server, it will bind to localhost:8081
 - Run ThriftPlainClient, it will access the standalone server
 - To see full trace, ensure the -javaagent argument is added to both of the processes
 
2. Non-blocking standalone Thrift server and client
 - Run ThriftNonBlockingServer as a standalone server, it will bind to localhost:8081
 - Run ThriftNonBlockingClient, it will access the standalone server
 - To see full trace, ensure the -javaagent argument is added to both of the processes
 
3. Thrift as a servlet
 - Build and deploy the project to a web app server, this will serve Thrift as servlet (port 8080 - standard webapp port for most app servers)
 - Run ThriftServletClient, it will access the thrift servlet deployed (8080 - adjust port if the server is bind to non 8080 port)
 - To see full trace, ensure the -javaagent argument is added to both the webapp server and the ThriftServletClient process
 
Take note that further tweaking might be needed as the server seems to stall sometimes with a lot of requests