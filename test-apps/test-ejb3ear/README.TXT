1. on project test-ejb3:
mvn clean install
2. on this project:
mvn clean package
3. drop the ear file from target folder to the "deploy" folder on jboss
4. start jboss, it should bind the this as ejbname : TestEJB3 and shows:

13:10:00,233 INFO  [EJBContainer] STARTED EJB: com.tracelytics.test.ejb3.TestEJB3 ejbName: TestEJB3
13:10:00,252 INFO  [JndiSessionRegistrarBase] Binding the following Entries in Global JNDI:

	TestEJB3/remote - EJB3.x Default Remote Business Interface
	testejb3-ear-1.0/TestEJB3/remote-com.tracelytics.test.ejb3.TestEJB3Remote - EJB3.x Remote Business Interface

5. Make sure memcached is installed on the machine as the testing endpoint makes use of memcached. To install memcached use
apt-get install memcached

6. If encounter ClassDefNotFoundException on SerializingTranscoder, it is probably due to sl4j version mismatch of JBOSS (had this problem on jboss 6) and XMemcached. 
Try to remove the slf4j.jar from within testejb3-ear-1.0.ear file