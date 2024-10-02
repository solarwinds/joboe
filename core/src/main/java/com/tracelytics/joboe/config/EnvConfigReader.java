package com.tracelytics.joboe.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.tracelytics.util.ServiceKeyUtils;

/**
 * Reads from system environment variables for {@link ConfigProperty}
 * 
 * @author Patson Luk
 *
 */
public class EnvConfigReader extends ConfigReader {
    private Map<String, String> env;

    public EnvConfigReader(Map<String, String> env) {
        super(ConfigSourceType.ENV_VAR);
        this.env = env;
    }

    public void read(ConfigContainer container) throws InvalidConfigException {
        List<InvalidConfigException> exceptions = new ArrayList<InvalidConfigException>();
        for (Entry<String, ConfigProperty> envNameEntry : ConfigProperty.getEnviromentVariableMap().entrySet()) {
            String envName = envNameEntry.getKey();
            if (env.containsKey(envName)) {
                String value = env.get(envName);
                try {
                    container.putByStringValue(envNameEntry.getValue(), value);
                    
                    String maskedValue;
                    if (envNameEntry.getValue() == ConfigProperty.AGENT_SERVICE_KEY) {
                        maskedValue = ServiceKeyUtils.maskServiceKey(value);
                    } else {
                        maskedValue = value;
                    }
                    logger.info("System environment variable [" + envName + "] value [" + maskedValue + "] maps to agent property " + envNameEntry.getValue());
                } catch (InvalidConfigException e) {
                    logger.warn("Invalid System environment variable [" + envName + "] value [" + value + "]");
                    exceptions.add(e);
                }
            }
        }

        if (!exceptions.isEmpty()) {
            logger.warn("Found " + exceptions.size() + " exception(s) while reading config from environment variables");
            throw exceptions.get(0); //report the first exception encountered
        }
    }
}
