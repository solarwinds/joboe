The app is accessible by default at http://<host>/test-hbase/ (latest supported version 1.1.1)

for example:
http://localhost:8080/test-hbase/

In order to run hbase server, please download the components here:
http://www.apache.org/dyn/closer.cgi/hbase/

Unpack the package and then follow steps as below (full quick-start here http://hbase.apache.org/book/quickstart.html):

For simplest stand-alone mode:
1. Goto the unpacked location. then start hbase with ./bin/start-hbase.sh
2. If it complains JAVA HOME not set, then just goto conf/hbase-env.sh and uncomment the export JAVA_HOME line
3. ./bin/hbase shell  (shell mode) 
4. create 'test', {NAME => 'cf'}, {NAME => 'blob', VERSIONS => 1} 
5. Go back to the testing app, it should be able to access the testing hbase server and table. If it is still not working, check the log files under ./logs/


For pseudo-distributed mode (fully-distributed mode run on a single host)
1. Also install Hadoop the HDFS http://hadoop.apache.org/#Getting+Started . Just do the single node setup here (use 2.x version)
If it complains about JAVA_HOME not set, set it in <installation directory>/etc/hadoop/hbase-env.sh
2. Configure Hadoop in <installation directory>/etc/hadoop/core-site.xml . Replace with config as below (!!! replace the hdfs location with your user !!!!) 
<configuration>
     <property>
         <name>fs.default.name</name>
         <value>hdfs://localhost:9000</value>
     </property>
     <property>
         <name>hadoop.tmp.dir</name>
         <value>/home/!!!REPLACE WITH YOUR USER!!!/hdfs/tmp</value>
     </property>
</configuration>
3. Start hadoop by ./bin/start-dfs.sh
4. Check hadoop is up and running - ./bin/hadoop fs -du /
Should show the contents of the dfs (could be empty now, but should have hbase later), it should not return connection error 


5. Goto the hbase installation directory, edit conf/hbase-site.xml:
<configuration>
  <property>
    <name>hbase.rootdir</name>
    <value>hdfs://localhost:9000/hbase</value>
  </property>
  <property>
    <name>hbase.cluster.distributed</name>
    <value>true</value>
  </property>
  <!-- small filesize to force more region creations -->
  <property>
    <name>hbase.hregion.max.filesize</name>
    <value>1048576</value>
  </property>
  <property>
    <name>hbase.table.sanity.checks</name>
    <value>false</value>
  </property>
  <!-- for coprocessor method testing -->
  <property>
    <name>hbase.coprocessor.region.classes</name>
    <value>org.apache.hadoop.hbase.coprocessor.AggregateImplementation</value>
   </property>
   <property>
    <name>hbase.zookeeper.quorum</name>
    <value>hbasehost</value>
   </property>
   <property>
    <name>hbase.master</name>
    <value>hbasehost:60000</value>
   </property>
</configuration>

**Setup host name "hbasehost" in the etc/hosts. If both server and client are deployed on the same machine, it can just map to 127.0.0.1. Otherwise it should use the public ip of the server. (this has to be done on both the server and client machine)

6. Start hbase with ./bin/start-hbase.sh
7. Start more region servers with ./bin/local-regionservers.sh start 2 3
8. Now goto http://127.0.0.1:16010.
9. ./bin/hbase shell  (shell mode) 
10. create 'test', {NAME => 'cf'}, {NAME => 'blob', VERSIONS => 1}	
11. Go back to the testing app, it should be able to access the testing hbase server and table. If it is still not working, check the log files under ./logs/ 
12. Try to add more rows into the table from testing app. Monitor the "Load" column of "Region Servers" in http://127.0.0.1:16010
13. If enough rows are added (try the max-value 999999 on the Test Put rows op), the regions will spread from the first default server to other servers. To speed up this process goto hbase shell and

balance_switch true
balancer

** steps below delete all your hbase table and hadoop data, use with caution**
1. Kill all hadoop and hbase process
pkill -9 -f hbase
pkill -9 -f hadoop

2. Delete tmp data of hbase (this keeps your metadata) - it's in /tmp/hbase-<username>
3. Delete the hadoop phyiscal directory - usually in /home/<user>/hdfs
4. Format the hadoop again - <hadoop directory>/bin/hadoop namenode -format
5. Restart hadoop. Make sure the namenode is fine. Inspect log file in <hadoop directory>/logs/hadoop-root-namenode-...log
6. Restart hbase. Make sure the master node is working. Inspect log file in <hbase directory>/logs/hbase-root-master-...log