This is a Webapp for testing Redis client Jedis (version 3.5.2)
https://github.com/xetorthio/jedis

Use "mvn clean package" to build the jar and run the jar which binds the test app to localhost:8080

The app can be accessed via: 
http://localhost:8080

The test app requires a running Redis server on localhost on default port (6379)
To setup Redis server, please follow instruction in:
http://redis.io/topics/quickstart

**To test on sharding/partition (client side):**
Another Redis instance for client side sharding test is available (52.7.124.5:1250 and 52.7.124.5:1252)

Take note that this tests on client side sharding https://redis.io/topics/partitioning#clients-supporting-consistent-hashing, which the extra instance here is simple redis instance (w/o knowing that it's a partition/shard)

To setup shard manually, simply start another Redis server instance on localhost with a different port
This could be done by creating an alternative redis configuration file and start a new redis instance with it.

In the testing app, under "Shard Test", use space separated host string such as `52.7.124.5:1250 52.7.124.5:1252`

**To test on cluster:**
Test cluster is available at "Redis Cluster" at https://github.com/librato/trace/blob/master/docs/testing/Supporting-Services-for-Testing.md#docker-services (52.7.124.5 port 17000 - 17004)

To set up cluster manually, please follow instruction in http://redis.io/topics/cluster-tutorial

In the testing app, under "Cluster Test", use space separated host string such as `52.7.124.5:17000 52.7.124.5:17001 52.7.124.5:17002`


**To test on sentinel:**
Test sentinel is available at (sentinel at 52.7.124.5:1253, point at the cluster 52.7.124.5:17000)

To set up sentinel manually, it requires as least 1 master and 1 slave, to set up slave server, simply create a new redis config with a line
replicaof <master host> <master port>
More details in http://redis.io/topics/replication

The should just one sentinel with port 26379. Also in the sentinel config, make sure the quorom (the last param) is 1 (as there is only 1 sentinel) as below:
sentinel monitor mymaster 127.0.0.1 6379 1

Use master name `mymaster`

In the testing app, under "Sentinel Test", enter the sentinel host and port, default as `52.7.124.5:1253`
