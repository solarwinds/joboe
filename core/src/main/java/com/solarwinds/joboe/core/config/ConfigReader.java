package com.solarwinds.joboe.core.config;

import com.solarwinds.joboe.logging.Logger;
import com.solarwinds.joboe.logging.LoggerFactory;
import lombok.Getter;

public abstract class ConfigReader {
    protected final Logger logger = LoggerFactory.getLogger();
    @Getter
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

}