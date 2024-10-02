The app is accessible by default at http://<host>/test-cassandra-datastax-min/ (Min supported driver - Cassandra Datastax CQL driver 1.0.0) 

for example:
http://localhost:8080/test-cassandra-datastax-min/

1. Set up cassandra server, this client only supports up to Cassandra server 1.2.x (1.1 does NOT work as it does not support native CQL connection, which binds to port 9042 by default)
, this client would NOT run on Cassandra server 2+. Download the Cassandra server from http://cassandra.apache.org/download/
2. Extract the downloaded client to a local directory, then follow instructions in https://wiki.apache.org/cassandra/GettingStarted
3. The testing app will by default point to Cassandra node of "localhost" If you wish to test the client on a different host from the node, enter the server host in the "Change hosts..." section of the testing app

To test on multiple node environment:
CQL can connect to one cluster at a time and each of the Cassandra cluster can contain multiple nodes, data(rows) would be replicated among nodes based on the replication factor
More details:
http://www.datastax.com/docs/1.1/dml/data_consistency
http://www.datastax.com/docs/1.0/cluster_architecture/replication

To setup multiple nodes, follow instruction in 
http://www.datastax.com/documentation/cassandra/2.0/cassandra/initialize/initializeSingleDS.html


Then after all the nodes are started up, add the list of reachable hosts (at least one of the nodes) of the cluster to our testing app in the "Change hosts..." section. 

Any query executed will then use the list of hosts, and the coordinating host can be found in the "RemoteHost" KV

