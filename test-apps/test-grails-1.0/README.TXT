This is a testing webapp of Grails 1.0. Take note that we do NOT really supprt Grails 1.0

However, our instrumentation does capture the top level Grails extent as well as the Spring-render of the view


1. Install the Grails v1.0 framework to the machine
2. Setup the environment variables to make sure they are pointing at this version
3. run grails war, the war will be created in the target folder
4. deploy the war to app container
5. testing endpoint is available at the deployed app root, for example:
http://localhost:8080/test-grails-1.0/
6. to test controller and filter, click the com.tracelytics.HelloController in the testing endpoint
