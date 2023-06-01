package com.tracelytics.joboe.config;

/**
 * Invalid config while reading from a specific {@Link ConfigSourceType}
 *
 * This contains extra info on the source type read and a ConfigContainer with the config read so far
 */
public class InvalidConfigReadSourceException extends InvalidConfigException {

    /**
     *
     */
    private static final long serialVersionUID = 1L;
    private ConfigSourceType configSourceType;
    private ConfigContainer configContainerBeforeException = null;
    private String physicalLocation = null;

    public InvalidConfigReadSourceException(ConfigProperty configProperty, ConfigSourceType sourceType, String physicalLocation, ConfigContainer configContainerBeforeException, InvalidConfigException exception) {
        super(configProperty, exception.originalMessage, exception);
        this.physicalLocation = physicalLocation;
        this.configSourceType = sourceType;
        this.configContainerBeforeException = configContainerBeforeException;
        this.configProperty = exception.getConfigProperty();
    }

    public ConfigContainer getConfigContainerBeforeException() {
        return configContainerBeforeException;
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
            case JVM_ARG: //cannot use String.join due to 1.6 compatibility
                boolean isFirstKey = true;
                StringBuilder argumentList = new StringBuilder();
                for (String key : configProperty.getAgentArgumentKeys()) {
                    if (!isFirstKey) {
                        argumentList.append("/");
                    }
                    argumentList.append(key);
                    isFirstKey = false;
                }
                return argumentList.toString();
            case JSON_FILE: return  configProperty.getConfigFileKey();
            default: return configProperty.name();
        }
    }

    private static String getLocation(ConfigSourceType configSourceType, String physicalLocation) {
        switch (configSourceType) {
            case ENV_VAR: return "Environment variable";
            case JVM_ARG: return "JVM agent argument";
            case JSON_FILE: return "JSON config file at " + physicalLocation;
            default: return "Unknown location";
        }
    }
}
