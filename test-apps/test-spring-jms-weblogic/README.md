# How to use

This README describes how can you set up a demo application environment to test JMS on WebLogic.

1. Set up the WebLogic env.
    Note: This step can be skipped as there is a WebLogic server installed and configured on http://ec2-34-233-45-163.compute-1.amazonaws.com:7001/console
    If you need to install and configure it by yourself, checkout this doc: https://docs.oracle.com/javacomponents/advanced-management-console-2/install-guide/oracle-weblogic-server-installation-example.htm#JSAMI208
    
2. Set up a JMS queue in the WebLogic.
   Note: This step can also be skipped as there is a JMS queue created. However, read this blog https://blogs.oracle.com/fusionmiddlewaresupport/jms-step-1-how-to-create-a-simple-jms-queue-in-weblogic-server-11g-v2 if you'd like to create a new one.
   
3. Build the package.
    Run `mvn clean package` to build the war package. The war file is under the directory `./target`.
    
4. Go to the WebLogic management console above and deploy it in `mydomain`->`Deployments`, and make sure the status of the app is `Active` (rather than `Prepared` or something else)

Now the demo app is ready.

Access `http://ec2-34-233-45-163.compute-1.amazonaws.com:7003/wl-jms-spring/send` to produce a JMS message.
Access `http://ec2-34-233-45-163.compute-1.amazonaws.com:7003/wl-jms-spring/consume` to consume it.
