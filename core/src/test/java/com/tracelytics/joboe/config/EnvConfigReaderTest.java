package com.tracelytics.joboe.config;

import junit.framework.TestCase;

import java.util.HashMap;
import java.util.Map;

public class EnvConfigReaderTest extends TestCase {
    public void testValidRead() throws InvalidConfigException {
        Map<String, String> vars = new HashMap<String, String>();
        vars.put("APPOPTICS_SERVICE_KEY", "some key");
        vars.put("APPOPTICS_SQL_SANITIZE", "2");
        EnvConfigReader reader = new EnvConfigReader(vars);
        ConfigContainer container = new ConfigContainer();
        reader.read(container);

        assertEquals("some key", container.get(ConfigProperty.AGENT_SERVICE_KEY));
        assertEquals(2, container.get(ConfigProperty.AGENT_SQL_SANITIZE));
    }

    /**
     * Even if some values are invalid, it should still read the rest
     */
    public void testPartialRead() {
        Map<String, String> vars = new HashMap<String, String>();
        vars.put("APPOPTICS_SERVICE_KEY", "some key");
        vars.put("APPOPTICS_SQL_SANITIZE", "2.1");
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
