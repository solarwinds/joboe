package com.solarwinds.joboe.config;

import lombok.Getter;

/**
 * Invalid config while reading from a specific {@Link ConfigSourceType}
 *
 * This contains extra info on the source type read and a ConfigContainer with the config read so far
 */
public class InvalidConfigReadSourceException extends InvalidConfigException {
    private static final long serialVersionUID = 1L;
    private final ConfigSourceType configSourceType;
    @Getter
    private ConfigContainer configContainerBeforeException = null;
    private String physicalLocation = null;

    public InvalidConfigReadSourceException(ConfigProperty configProperty, ConfigSourceType sourceType, String physicalLocation, ConfigContainer configContainerBeforeException, InvalidConfigException exception) {
        super(configProperty, exception.originalMessage, exception);
        this.physicalLocation = physicalLocation;
        this.configSourceType = sourceType;
        this.configContainerBeforeException = configContainerBeforeException;
        this.configProperty = exception.getConfigProperty();
    }


    @Override
    public String getMessage() {
        if (configProperty == null && configSourceType == null) {
            return super.getMessage();
        } else {
            StringBuilder message = new StringBuilder("Found error in config. ");
            if (configSourceType != null) {
                message.append("Location: " + getLocation(configSourceType, physicalLocation) + ".");
            }
            if (configProperty != null) {
                message.append("Config key: " + (configSourceType != null ? getConfigPropertyLabel(configSourceType, configProperty) : configProperty.name()) + ".");
            }

            message.append(" " + originalMessage);
            return message.toString();
        }
    }

    private static String getConfigPropertyLabel(ConfigSourceType configSourceType, ConfigProperty configProperty) {
        switch (configSourceType) {
            case ENV_VAR: return configProperty.getEnvironmentVariableKey();
            case JSON_FILE: return  configProperty.getConfigFileKey();
            default: return configProperty.name();
        }
    }

    private static String getLocation(ConfigSourceType configSourceType, String physicalLocation) {
        switch (configSourceType) {
            case ENV_VAR: return "Environment variable";
            case JSON_FILE: return "JSON config file at " + physicalLocation;
            default: return "Unknown location";
        }
    }
}
