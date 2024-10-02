This is a Webapp for testing Redis client Redisson (latest version 3.14.0)
https://github.com/xetorthio/jedis

Use "mvn clean package" to build the war and deploy to app server

The app can be accessed via: 
http://localhost:8080/test-redis-redisson/


**To test on cluster:**
Please follow instruction in http://redis.io/topics/cluster-tutorial
There should be one master node with port 7000 (as in the tutorial as the port number is hardcoded in com.tracelytics.test.action.TestCluster)

**To test on master-slave:**
To set up slave sever, simply create a new redis config with a line

slaveof 127.0.0.1 6379

Then start another redis server instance with that new config
More details in http://redis.io/topics/replication

**To test on sentinel:**
It requires as least 1 master and 1 slave as described above in "To test on master-slave" 

To setup sentinel, please follow instruction in http://redis.io/topics/sentinel
The should just one sentinel with port 26379 (as in the tutorial as the port number is hardcoded in com.tracelytics.test.action.TestSentinel)
Also in the sentinel config, make sure the quorom (the last param) is 1 (as there is only 1 sentinel) as below:
sentinel monitor mymaster 127.0.0.1 6379 1
