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
Please follow instruction in http://redis.io/topics/cluster-tutorial
There should be one master node with port 7000 (as in the tutorial as the port number is hardcoded in com.tracelytics.test.action.TestCluster)

**To test on sentinel:**
It requires as least 1 master and 1 slave, to set up slave sever, simply create a new redis config with a line
slaveof 127.0.0.1 6379
More details in http://redis.io/topics/replication
To setup sentinel, please follow instruction in http://redis.io/topics/sentinel
The should just one sentinel with port 26379 (as in the tutorial as the port number is hardcoded in com.tracelytics.test.action.TestSentinel)
Also in the sentinel config, make sure the quorom (the last param) is 1 (as there is only 1 sentinel) as below:
sentinel monitor mymaster 127.0.0.1 6379 1