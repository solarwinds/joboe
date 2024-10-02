This is related to https://github.com/tracelytics/joboe/issues/354#issuecomment-72925591 , that JDK8 and javassist has conflicts that trigger server crash reported by Hubspot https://tickets.appneta.com/helpdesk/tickets/4361

To reproduce the problem:
1. Build the testing agent by running on this project folder:
mvn clean package
2. Ensure the current java version is jdk 8. Confirm this by:
java -version
3. Compile TestJavaScript.java at src folder:
javac test/TestJavaScript.java
4. java -cp . -javaagent:..\target\test-jdk8-agent-0.0.1-SNAPSHOT-jar-with-dependencies.jar test.TestJavaScript
5. Repeat step 4 several times, the JVM will eventually crash at different places
6. To check on our agent, simply replace the -javaagent argument with the agent jar location. It should no longer crash after the fix in version 3.3.2



The problem has as well been reported to Oracle, but no reply from them yet
 

