Servlet that runs on Tomcat and calls into an EJB3 running on JBoss 7.

Refer to test-ejb3ear for deploying EJB3 service on JBoss

Then deploy this webapp to Tomcat (JBoss ear should take 8081) and access thru:

http://localvm:8080/testejb3-webapp-jboss7/test
http://localvm:8080/testejb3-webapp-jboss7/test-async (take note that in jboss 7 the async is not really running as async as it always returns the sycnrhonous handler - probably a jboss bug)

Take note that a user is used to authenticated (in TestServlet):
props.put(Context.SECURITY_PRINCIPAL, "testuser1");
props.put(Context.SECURITY_CREDENTIALS, "testpass1");


Therefore please setup a user in jboss as below:

root@ip-10-80-15-145:/usr/share/jboss-7.1.1# bin/add-user.sh

What type of user do you wish to add?
 a) Management User (mgmt-users.properties)
 b) Application User (application-users.properties)
(a): b

Enter the details of the new user to add.
Realm (ApplicationRealm) :
Username : testuser1
Password : testpass1
Re-enter Password : testpass1
What roles do you want this user to belong to? (Please enter a comma separated list, or leave blank for none) :
About to add user 'testuser1' for realm 'ApplicationRealm'
Is this correct yes/no? yes
Added user 'testuser1' to file '/usr/share/jboss-7.1.1/standalone/configuration/application-users.properties'
Added user 'testuser1' to file '/usr/share/jboss-7.1.1/domain/configuration/application-users.properties'
