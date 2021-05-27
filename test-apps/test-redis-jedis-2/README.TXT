This is a Webapp for testing Redis client Jedis (latest version 2.6.1)
https://github.com/xetorthio/jedis

Use "mvn clean package" to build the war and deploy to app server

The app can be accessed via: 
http://localhost:8080/test-redis-jedis-2/

The test app requires a running Redis server on localhost on default port (6379)
To setup Redis server, please follow instruction in:
http://redis.io/topics/quickstart

**To test on the shards:** 
please start another Redis server instance on localhost with a different port (6380, hardcoded in com.tracelytics.test.action.TestShard)
This could be done by creating an alternative redis configuration file and start a new redis instance with it.

The alternative config file should have it port line set to:
port 6380

Start both then original Redis server instance and another alternate Redis server instance using the new config file

**To test on cluster:**
The code by default points at cluster on ec2-52-7-124-5.compute-1.amazonaws.com, which is a shared testing service deployed on EC2. See https://github.com/librato/trace/blob/master/docs/testing/Supporting-Services-for-Testing.md#docker-services for details

**To test on sentinel:**
Similar to cluster, it by default points at sentinel setup on ec2-52-7-124-5.compute-1.amazonaws.com

More details in http://redis.io/topics/replication
To setup sentinel locally, please follow instruction in http://redis.io/topics/sentinel
The should just one sentinel with port 26379 (as in the tutorial as the port number is hardcoded in com.tracelytics.test.action.TestSentinel)
Also in the sentinel config, make sure the quorom (the last param) is 1 (as there is only 1 sentinel) as below:
sentinel monitor mymaster 127.0.0.1 6379 1
