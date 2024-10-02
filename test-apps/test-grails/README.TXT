This is an testing application for Grails v2.3.6 instrumentation

1. Install the Grails v2.3.6 framework to the machine (only runs on JDK 7, using JDK 8 might result as compilation error in build process)
2. Setup the environment variables to make sure they are pointing at this version
3. run grails war, the war will be created in the target folder
4. deploy the war to app container
5. testing endpoint is available at the deployed app root, for example:
http://localhost:8080/test-grails/
6. to test controller and filter, click the com.tracelytics.HelloController in the testing endpoint

To run in embedded tomcat use:
1. Configure C:\Users\pluk\git\joboe\test-apps\test-grails\grails-app\conf\BuildConfig.groovy , uncomment last line, use the correct agent jar location
2. `grails RunApp`
