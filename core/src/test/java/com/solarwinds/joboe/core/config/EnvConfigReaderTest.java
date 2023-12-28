package com.solarwinds.joboe.core.config;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class EnvConfigReaderTest {
    @Test
    public void testValidRead() throws InvalidConfigException {
        Map<String, String> vars = new HashMap<String, String>();
        vars.put(ConfigProperty.EnvPrefix.PRODUCT + "SERVICE_KEY", "some key");
        EnvConfigReader reader = new EnvConfigReader(vars);
        ConfigContainer container = new ConfigContainer();
        reader.read(container);

        assertEquals("some key", container.get(ConfigProperty.AGENT_SERVICE_KEY));
    }

    /**
     * Even if some values are invalid, it should still read the rest
     */
    @Test
    public void testPartialRead() {
        Map<String, String> vars = new HashMap<String, String>();
        vars.put(ConfigProperty.EnvPrefix.PRODUCT + "SERVICE_KEY", "some key");
        vars.put(ConfigProperty.EnvPrefix.PRODUCT + "MAX_SQL_QUERY_LENGTH", "2.1");
        EnvConfigReader reader = new EnvConfigReader(vars);
        ConfigContainer container = new ConfigContainer();
        try {
            reader.read(container);
            fail("Expected " + InvalidConfigException.class.getName() + " but it's not thrown");
        } catch (InvalidConfigException e) {
            //expected
        }

        assertEquals("some key", container.get(ConfigProperty.AGENT_SERVICE_KEY));
    }
}
