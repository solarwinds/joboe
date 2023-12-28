package com.solarwinds.joboe.core.config;

import lombok.Getter;

public class InvalidConfigException extends Exception {
    private static final long serialVersionUID = 1L;
    protected String originalMessage;
    @Getter
    protected ConfigProperty configProperty;

    public InvalidConfigException(String message) {
        this(message, null);
    }

    public InvalidConfigException(Throwable cause) {
        this(null, cause);
    }

    public InvalidConfigException(String message, Throwable cause) {
        this(null, message, cause);
    }

    public InvalidConfigException(ConfigProperty configProperty, String message) {
        this(configProperty, message, null);
    }

    public InvalidConfigException(ConfigProperty configProperty, String message, Throwable cause) {
        super(message, cause);
        this.originalMessage = message;
        this.configProperty = configProperty;
    }


    @Override
    public String getMessage() {
        if (configProperty == null) {
            return super.getMessage();
        } else {
            return "Found error in config. Config key: " + configProperty.name() +
                    " " + super.getMessage();
        }
    }
}
