Servlet that runs on Tomcat and calls into an EJB3 running on JBoss 8 (wildfly).

Refer to test-ejb3ear for deploying EJB3 service on JBoss

Then deploy this webapp to Tomcat (JBoss ear should take 8081) and access thru:

http://localvm:8080/testejb3-webapp-wildfly/test
http://localvm:8080/testejb3-webapp-wildfly/test-async


Take note that a user is used to authenticated (in TestServlet):
props.put(Context.SECURITY_PRINCIPAL, "testuser1");
props.put(Context.SECURITY_CREDENTIALS, "testpass1");


Therefore please setup a user in jboss as below:

root@ip-10-80-15-145:/usr/share/jboss-8.1.0.Final# bin/add-user.sh

What type of user do you wish to add?
 a) Management User (mgmt-users.properties)
 b) Application User (application-users.properties)
(a): b

Enter the details of the new user to add.
Realm (ApplicationRealm) :
Username : testuser1
Password : testpass1
Re-enter Password : testpass1
What groups do you want this user to belong to? (Please enter a comma separated list, or leave blank for none)[  ]: 
About to add user 'testuser1' for realm 'ApplicationRealm'
Is this correct yes/no? yes
Added user 'testuser1' to file '/usr/share/jboss-8.1.0.Final/standalone/configuration/application-users.properties'
Added user 'testuser1' to file '/usr/share/jboss-8.1.0.Final/domain/configuration/application-users.properties'
Added user 'testuser1' with groups  to file '/usr/share/jboss-8.1.0.Final/standalone/configuration/application-roles.properties'
Added user 'testuser1' with groups  to file '/usr/share/jboss-8.1.0.Final/domain/configuration/application-roles.properties'
Is this new user going to be used for one AS process to connect to another AS process? 
e.g. for a slave host controller connecting to the master or for a Remoting connection for server to server EJB calls.
yes/no? no
