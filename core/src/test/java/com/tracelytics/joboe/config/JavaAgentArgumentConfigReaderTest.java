package com.tracelytics.joboe.config;

import junit.framework.TestCase;

public class JavaAgentArgumentConfigReaderTest extends TestCase {
    public void testValidRead() throws InvalidConfigException {
        JavaAgentArgumentConfigReader reader = new JavaAgentArgumentConfigReader("logging=debug,config=abc.text");
        ConfigContainer container = new ConfigContainer();
        reader.read(container);

        assertEquals("debug", container.get(ConfigProperty.AGENT_LOGGING));
        assertEquals("abc.text", container.get(ConfigProperty.AGENT_CONFIG));
    }

    /**
     * Even if some values are invalid, it should still read the rest
     */
    public void testPartialRead() {
        JavaAgentArgumentConfigReader reader = new JavaAgentArgumentConfigReader("unknown-key=abc,logging=debug,config=abc.text");
        ConfigContainer container = new ConfigContainer();
        try {
            reader.read(container);
            fail("Expected " + InvalidConfigException.class.getName() + " but it's not thrown");
        } catch (InvalidConfigException e) {
            //expected
        }

        assertEquals("debug", container.get(ConfigProperty.AGENT_LOGGING));
        assertEquals("abc.text", container.get(ConfigProperty.AGENT_CONFIG));
    }
}
