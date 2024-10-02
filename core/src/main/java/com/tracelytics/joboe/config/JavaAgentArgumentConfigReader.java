package com.tracelytics.joboe.config;

import java.util.ArrayList;
import java.util.List;

/**
 * A read that reads the arguments passed in via -javaagent
 * @author Patson Luk
 *
 */
public class JavaAgentArgumentConfigReader extends ConfigReader {

    private String agentArgs;

    
    public JavaAgentArgumentConfigReader(String agentArgs) {
        super(ConfigSourceType.JVM_ARG);
        this.agentArgs = agentArgs;
    }


    /**
     * Code extracted and refactored from <code>Agent.parseArgs(String)</code>
     */
    public void read(ConfigContainer container) throws InvalidConfigException {
        if (agentArgs == null) {
            return;
        }
        
        String args[] = agentArgs.split(",");

        List<InvalidConfigException> exceptions = new ArrayList<InvalidConfigException>();
        for(String arg: args) {
            String nv[] = arg.split("=");
            String argName = nv[0].toLowerCase();
            String argVal = nv.length > 1 ? nv[1] : "";

            ConfigProperty property = ConfigProperty.fromAgentArgumentKey(argName);
            
            if (property != null) {
                try {
                    container.putByStringValue(property, argVal);
                } catch (InvalidConfigException e) {
                    logger.warn("Invalid java agent param [" + argName + "] value [" + argVal + "]");
                    exceptions.add(e);
                }
            } else {
                exceptions.add(new InvalidConfigException("Unexpected argument [" + argName + "]"));
            }
        }

        if (!exceptions.isEmpty()) {
            logger.warn("Found " + exceptions.size() + " exception(s) while reading config from java argument");
            throw exceptions.get(0); //report the first exception encountered
        }
    }
    

    

}
