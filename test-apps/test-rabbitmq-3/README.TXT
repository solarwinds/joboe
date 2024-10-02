A simple app to test on Rabbit-MQ driver (version 3.6.6)

1. Install and run Rabbit MQ.
2. Goto http://localhost:8080/test-rabbitmq-3
3. In the "Start Rabbit MQ Consumers" section. Enter the host (required) and the port (optional) values accordingly and click "Start Consumers". This will bind the consumers to the queues.
4. In the "Send Rabbit MQ Message" section. Enter the host (required) and the port (optional) values accordingly. 
5. Then enter the routing key which should either be "wait", "async_wait" or "echo". This determines which queue will the message be sent to, hence what consumer will take the message and process it
6. Also enter the message, for "wait" and "async_wait" routing key, use an integer string. This translates to how much time in millisec would the consumer use up for processing.
7. Choose whether to send it off as a publish (no callback, the client send the message out and move on) or as rpc (the client send the message out and then wait for the response in the reply queue)

The MQ structure of the test case is described in the test-mq.png of this project

IMPORTANT: Make sure the app container fully support websocket, for tomcat the implementation was provided since version 7.0.47 


