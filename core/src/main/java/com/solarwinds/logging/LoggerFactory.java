package com.solarwinds.logging;

import com.solarwinds.joboe.config.ConfigContainer;

/**
 * Factory that returns logger instance. Take note that this is very similar to other logging frameworks (commons.logging, log4j etc) factory and 
 * should be replaced by those factory if we later on switch to those frameworks
 * @author Patson Luk
 *
 */
public class LoggerFactory {
    //Only allow a single logger. We do not need multiple loggers as we handle everything using the same handlers at this moment
    //Take note that we do not instantiate this in the init routine as we wish to make the logger always available (even before init is run)
    //Just that code that uses the logger before calling init will not follow the configuration defined in the configration file <code>ConfigProperty.AGENT_DEBUG</code>
    private static Logger logger = Logger.INSTANCE;
    
    /**
     * Initialize the logger factory. This is used to set the logging level of the logger
     * @param config
     */
    public static void init(ConfigContainer config) {
        logger.configure(config); 
    }
    
    
   
    /**
     * 
     * @return a logger from this factory
     */
    public static final Logger getLogger() {
        return logger;
    }
}
