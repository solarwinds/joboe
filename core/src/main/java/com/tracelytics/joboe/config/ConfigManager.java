package com.tracelytics.joboe.config;

import com.tracelytics.logging.Logger;
import com.tracelytics.logging.LoggerFactory;

public class ConfigManager {
    private static final Logger logger = LoggerFactory.getLogger();
    private ConfigContainer configs;

    private static final ConfigManager SINGLETON = new ConfigManager(); //only a singleton for now

    private ConfigManager() {
    }

    public static void initialize(ConfigContainer configs) {
        SINGLETON.configs = configs;
    }

    /**
     * For internal testing - reset the states of the ConfigManager
     */
    public static void reset() {
        SINGLETON.configs = null;
    }

    public static void setConfig(ConfigProperty configKey, Object value) throws InvalidConfigException {
        if (SINGLETON.configs == null) {
            SINGLETON.configs = new ConfigContainer();
        }
        SINGLETON.configs.put(configKey, value, true);
    }

    public static void removeConfig(ConfigProperty configKey) {
        if (SINGLETON.configs != null) {
            SINGLETON.configs.remove(configKey);
        }
    }


    /**
     * Convenience method for other code to read the configuration value of the Agent
     *
     * @param configKey
     * @return the configuration value of the provided key. Take note that this might be null if the configuration property is not required
     */
    public static Object getConfig(ConfigProperty configKey) {
        if (SINGLETON.configs == null) {
            logger.warn("Failed to read config property [" + configKey + "] as agent is not initialized properly, config is null!");
            return null;
        }

        return SINGLETON.configs.get(configKey);
    }

    public static <T> T getConfigOptional(ConfigProperty configKey, T defaultValue) {
        if (SINGLETON.configs == null) {
            logger.warn("Failed to read config property [" + configKey + "] as agent is not initialized properly, config is null!");
            return defaultValue;
        }
        Object value = SINGLETON.configs.get(configKey);
        return value != null ? (T) value : defaultValue;
    }

    public static ConfigContainer getConfigs(ConfigGroup... groups) {
        if (SINGLETON.configs == null) {
            logger.warn("Agent is not initialized properly, config is null!");
            return new ConfigContainer();
        }

        return SINGLETON.configs.subset(groups);
    }
}

