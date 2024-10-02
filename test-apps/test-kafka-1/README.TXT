A simple app to test on Kafka clients/streams (1.0.0)

It will have a running Kafka broker (not as a part of the java application, it's a standalone program, see step 1, and
a testing UI which the user can add Kafka Client consumer and Kafka stream (another form of consumer) that listen to a
specific kafka topic. There's also an option to send a message to this topic with the Kafka Client producer.

General setup:
1. Install and run Kafka, create a topic following https://kafka.apache.org/quickstart#quickstart_createtopic
2. Goto http://localhost:8080/test-kafka-1

Start Consumer:
3. To test Kafka Clients(Consumer), start the Consumers with "Add Consumer" button, enter consumer group id (any custom string value)
and the topic created in step 1. The consumer will now be listen to the topic

Start Streams:
4. To test Kafka Streams(Consumer), start the Streams with the "Add Streams" button, enter the topic created in step 1.
The stream will now listen to the topic

Publish message to topic:
5. Sends the Message to the kafka broker with the same topic, message key is optional. The value could be any string value.
By default the consumer would take 1 second to process the message. If the message value is an integer value, it will be
interpreted as the sleep value on the processing. For example to force a 10 second process time, use 10000 as the message value

IMPORTANT: Make sure the app container fully support websocket, for tomcat the implementation was provided since version 7.0.47 


