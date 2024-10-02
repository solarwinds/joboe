# How to use
1. Build the jar with Maven: 
    `mvn clean package`
    
2. Run the jar with the AppOptics Java agent:
    `java -javaagent:/change_to_your_agent_path/appoptics-agent.jar=config=/change_to_agent_config_path/javaagent.json -jar gs-messaging-jms-0.1.0.jar`
   You'll see a Spring Boot application starts up and listens on `localhost:8080`

3. Access it to produce JMS messages:
    `curl -i http://localhost:8080/produce`
    
4. Now you can check out the JMS spans created by the demo application on the AppOptics dashboard.
