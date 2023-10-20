package com.tracelytics.joboe.config;

import junit.framework.TestCase;

public class JsonConfigReaderTest extends TestCase {
    public void testValidRead() throws InvalidConfigException {
        JsonConfigReader reader = new JsonConfigReader(getClass().getResourceAsStream("valid.json"));
        ConfigContainer container = new ConfigContainer();
        reader.read(container);

        assertEquals("info", container.get(ConfigProperty.AGENT_LOGGING));
        assertEquals("some key", container.get(ConfigProperty.AGENT_SERVICE_KEY));
    }

    /**
     * Even if some values are invalid, it should still read the rest
     */
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
