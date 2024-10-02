The app is accessible by default at http://<host>/test-mongodb/ (MongoDB java driver 3.12.0 / reactive streams driver 1.11.0)

for example:
http://localhost:8080/test-mongodb/

You would first need to setup mongodb locally. Simply follow the instructions in http://docs.mongodb.org/manual/installation/

Make sure your mongodb is running (on localhost:27017) when accessing this testing webapp


To setup shard cluster (http://docs.mongodb.org/manual/tutorial/deploy-shard-cluster/):

A shard cluster basically container 3 sets of modules:
1. Config servers
2. Query routers (route stuff for client to shard server)
3. Shard servers (the ones that actually keep the data, should be a replicate set in prod, but for testing, each shard can just be a single shard server)

1. Config servers
 -Create distinct data directory/port for each config server, for simple testing, only 1 is necessary
 -Create a conf file like below and start by "mongod -config config-server.conf
storage:
   dbPath: "C:/Users/pluk/Mongodb/config-server-1/data/configdb"
sharding:
   clusterRole: "configsvr"
net:
   port: 27021
   
2. Query routers
 -Do not need a data directory
 -The "endpoint" we captured for the Mongodb operations, therefore setup multiple routers to show varying "RemoteHost" values
 -Create a conf file like below and start by "mongos -config query-router.conf"
sharding:
   configDB: "localhost:27019"
net:
   port: 27017


3. Shard servers
 -Create distinct data directory/port for each shard server, for simple testing, only 1 is necessary
 -Create a conf file like below and start by "mongod -config shard-server.conf 
storage:
   dbPath: "C:/Users/pluk/Mongodb/shard-server-1/data"
sharding:
   clusterRole: "shardsvr"
net:
   port: 27023
   
4. Start the Config server(s) as listed above
5. Start the Query router(s) as listed above
6. Start the Shard server(s) as listed above
7. Add shard to cluster by linking the shard server to any of the Query routers, then enable sharding. For example
mongo --host localhost --port 27017
sh.addShard("localhost:27023")
sh.enableSharing("test")   // replace with your database name
8. Now change class AbstractMongoDbSyncAction/AbstractMongoDbAsyncAction, add to the HOST list all the Query routers.
9. Run the test-mongodb app, the "RemoteHost" captured should flip between different Query routers

 

