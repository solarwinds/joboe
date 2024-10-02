This is a Webapp for testing Redis client Jedis (min version 1.3.0)
https://github.com/xetorthio/jedis

Use "mvn clean package" to build the war and deploy to app server

The app can be accessed via: 
http://localhost:8080/test-redis-jedis-min/

The test app requires a running Redis server on localhost on default port (6379)
To setup Redis server, please follow instruction in:
http://redis.io/topics/quickstart

**To test on the shards:** 
please start another Redis server instance on localhost with a different port (6380, hardcoded in com.tracelytics.test.action.TestShard)
This could be done by creating an alternative redis configuration file and start a new redis instance with it.

The alternative config file should have it port line set to:
port 6380

Start both then original Redis server instance and another alternate Redis server instance using the new config file