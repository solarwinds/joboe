package com.solarwinds.joboe.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class JsonConfigReaderTest {
    @Test
    public void testValidRead() throws InvalidConfigException {
        JsonConfigReader reader = new JsonConfigReader(JsonConfigReaderTest.class.getResourceAsStream("valid.json"));
        ConfigContainer container = new ConfigContainer();
        reader.read(container);

        assertEquals("info", container.get(ConfigProperty.AGENT_LOGGING));
        assertEquals("some key", container.get(ConfigProperty.AGENT_SERVICE_KEY));
    }

    /**
     * Even if some values are invalid, it should still read the rest
     */
    @Test
    public void testPartialRead() {
        JsonConfigReader reader = new JsonConfigReader(getClass().getResourceAsStream("invalid.json"));
        ConfigContainer container = new ConfigContainer();
        try {
            reader.read(container);
            fail("Expected " + InvalidConfigException.class.getName() + " but it's not thrown");
        } catch (InvalidConfigException e) {
            //expected
        }

        //the rest of the values should be read
        assertEquals("info", container.get(ConfigProperty.AGENT_LOGGING));
        assertEquals("some key", container.get(ConfigProperty.AGENT_SERVICE_KEY));
    }
}
