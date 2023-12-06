package com.solarwinds.joboe.config;

import com.solarwinds.logging.Logger;
import com.solarwinds.logging.LoggerFactory;

public abstract class ConfigReader {
    protected final Logger logger = LoggerFactory.getLogger();
    private final ConfigSourceType configSourceType;

    protected ConfigReader(ConfigSourceType configSourceType) {
        this.configSourceType = configSourceType;
    }

    /**
     * Reads the configuration and puts the result in {@link ConfigContainer}
     * 
     * @param container 	the container which this config reader should write result into
     * @throws Exception
     */
	
    public abstract void read(ConfigContainer container) throws InvalidConfigException;

    public ConfigSourceType getConfigSourceType() {
        return configSourceType;
    }

}