## _spray_ test project on spray servlet 1.3.1

This project users Spray-servlet which deploys to Servlet container like tomcat


### Build the project war(optional)

either sbt or activator has to be installed on the system

1. To run the project, navigate to the root directory of the project (ie /home/my-user/git/joboe/test-apps/test-spray-servlet)
2. Do not need to run javaagent agent here, simply run command below
   sbt package
   OR
   activator package
3. The war file should be built in the target folder



### Deploy the project war

1. Drop the war file (either built from steps above or simply use the one in the war folder) to the servlet container for deployment (for example webapps folder of tomcat)
2. Start the servlet container app server (make sure the app server is instrumented)
3. goto http://localhost:8080/test-spray-servlet/ (or whatever the war name is) or http://localhost:8080/test-spray-servlet/order/123
   