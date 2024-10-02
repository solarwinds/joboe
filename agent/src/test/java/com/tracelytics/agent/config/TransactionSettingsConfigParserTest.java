package com.tracelytics.agent.config;

import com.tracelytics.joboe.SampleRateSource;
import com.tracelytics.joboe.config.InvalidConfigException;
import com.tracelytics.joboe.config.TraceConfigs;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class TransactionSettingsConfigParserTest {
    @Test
    public void testValidConfigs() throws InvalidConfigException {
        TraceConfigs configs;
        configs = TransactionSettingsConfigParser.INSTANCE.convert("[{ \"type\":\"url\", \"regex\" : \".*\", \"tracing\" : \"enabled\"}]");

        assertEquals(false, configs.getTraceConfig("abc").isSampleRateConfigured()); //for "enabled", the sample rate is undefined
        assertEquals(SampleRateSource.FILE, configs.getTraceConfig("abc").getSampleRateSource());
        assertEquals(true, configs.getTraceConfig("abc").hasSampleStartFlag());
        assertEquals(true, configs.getTraceConfig("abc").hasSampleTriggerTraceFlag());
        assertEquals(true, configs.getTraceConfig("abc").hasSampleThroughAlwaysFlag());

        configs = TransactionSettingsConfigParser.INSTANCE.convert("[{ \"type\":\"url\", \"regex\" : \".*\", \"tracing\" : \"disabled\"}]");

        assertEquals(0, configs.getTraceConfig("abc").getSampleRate()); //for "disabled" it's set to 0 sample rate
        assertEquals(false, configs.getTraceConfig("abc").hasSampleStartFlag());
        assertEquals(false, configs.getTraceConfig("abc").hasSampleTriggerTraceFlag());
        assertEquals(false, configs.getTraceConfig("abc").hasSampleThroughAlwaysFlag());

        //test on pattern matching
        configs = TransactionSettingsConfigParser.INSTANCE.convert("[{ \"type\":\"url\", \"regex\" : \".*\\\\.html\", \"tracing\" : \"disabled\"}]");
        assertTrue(configs.getTraceConfig("abc.html") != null);
        assertTrue(configs.getTraceConfig("abc.HTML") != null); //case insensitive
        assertTrue(configs.getTraceConfig("abc.HTML?abc") == null); //trailing contents do not match
        assertTrue(configs.getTraceConfig("abc.htm") == null);
        assertTrue(configs.getTraceConfig("abc-html") == null);

        //test on extension matching
        configs = TransactionSettingsConfigParser.INSTANCE.convert("[{ \"type\":\"url\", \"extensions\" : [\"png\", \"jpg\"], \"tracing\" : \"disabled\"}]");
        assertTrue(configs.getTraceConfig("abc.png") != null);
        assertTrue(configs.getTraceConfig("abc.PNG") != null); //case insensitive
        assertTrue(configs.getTraceConfig("abc.jpg") != null);
        assertTrue(configs.getTraceConfig("abc.html") == null);
        assertTrue(configs.getTraceConfig("abcpng") == null);
    }

    @Test
    /**
     * Test url that matches multiple entries, the first matching entry should take precedence
     * @throws InvalidConfigException
     */
    public void testPrecedence() throws InvalidConfigException {
        TraceConfigs configs = TransactionSettingsConfigParser.INSTANCE.convert(
                "[{ \"extensions\" : [\"png\", \"jpg\"], \"tracing\" : \"disabled\"}," +
                        " { \"type\":\"url\", \"regex\" : \".*legacy-type.*\", \"tracing\" : \"disabled\"}, " + //legacy entry with`type`, backward compatibility check
                        " { \"regex\" : \".*trace.*\", \"tracing\" : \"enabled\"}]");

        assertEquals(0, configs.getTraceConfig("trace.png").getSampleRate()); //disabled, as first entry has higher precedence
        assertEquals(false, configs.getTraceConfig("trace.png").hasSampleStartFlag());
        assertEquals(false, configs.getTraceConfig("trace.png").hasSampleTriggerTraceFlag());
        assertEquals(false, configs.getTraceConfig("trace.png").hasSampleThroughAlwaysFlag());

        assertEquals(0, configs.getTraceConfig("legacy-type-abc").getSampleRate()); //disabled, matching the 2nd legacy entry
        assertEquals(false, configs.getTraceConfig("legacy-type-abc").hasSampleStartFlag());
        assertEquals(false, configs.getTraceConfig("legacy-type-abc").hasSampleTriggerTraceFlag());
        assertEquals(false, configs.getTraceConfig("legacy-type-abc").hasSampleThroughAlwaysFlag());


        assertEquals(false, configs.getTraceConfig("trace.html").isSampleRateConfigured()); //enabled, matching only the last entry
        assertEquals(true, configs.getTraceConfig("trace.html").hasSampleStartFlag());
        assertEquals(true, configs.getTraceConfig("trace.html").hasSampleTriggerTraceFlag());
        assertEquals(true, configs.getTraceConfig("trace.html").hasSampleThroughAlwaysFlag());
    }


    @Test
    public void testInvalidConfigs() {
        //test empty config entry
        try {
            TransactionSettingsConfigParser.INSTANCE.convert("[{}]");
            fail("json is invalid, it should throw exceptions");
        } catch (InvalidConfigException e) {
            System.out.println("Found expected exception. Message: " + e.getMessage());
        }

        //invalid json, missing closing ]
        try {
            TransactionSettingsConfigParser.INSTANCE.convert("[{ \"regex\" : \".*\", \"tracing\" : \"enabled\"}");
            fail("Expect " + InvalidConfigException.class.getName() + " but found none");
        } catch (InvalidConfigException e) {
        }

        //invalid, should be an array
        try {
            TransactionSettingsConfigParser.INSTANCE.convert("{ \"regex\" : \".*\", \"tracing\" : \"enabled\"}");
            fail("Expect " + InvalidConfigException.class.getName() + " but found none");
        } catch (InvalidConfigException e) {
        }

        //unknown key : mode
        try {
            TransactionSettingsConfigParser.INSTANCE.convert("[{ \"regex\" : \".*\", \"mode\" : \"enabled\"}]");
            fail("Expect " + InvalidConfigException.class.getName() + " but found none");
        } catch (InvalidConfigException e) {
        }


        //no regex
        try {
            TransactionSettingsConfigParser.INSTANCE.convert("[{ \"tracing\" : \"enabled\"}]");
            fail("Expect " + InvalidConfigException.class.getName() + " but found none");
        } catch (InvalidConfigException e) {
        }

        //extensions should be an array
        try {
            TransactionSettingsConfigParser.INSTANCE.convert("[{ \"extensions\" : \"png\", \"tracing\" : \"disabled\"}]");
            fail("Expect " + InvalidConfigException.class.getName() + " but found none");
        } catch (InvalidConfigException e) {
        }

        //invalid tracing value
        try {
            TransactionSettingsConfigParser.INSTANCE.convert("[{ \"regex\" : \".*\", \"tracing\" : \"yes\"}]");
        } catch (InvalidConfigException e) {
        }

        //test with more than one match criterion
        try {
            TransactionSettingsConfigParser.INSTANCE.convert("[{ \"type\":\"url\", \"regex\" : \".*\", \"tracing\" : \"disabled\", \"extensions\" : [\"jpg\"] }]");
            fail("json is invalid, it should throw exceptions");
        } catch (InvalidConfigException e) {
            System.out.println("Found expected exception. Message: " + e.getMessage());
        }

        //test invalid regex syntax
        try {
            TransactionSettingsConfigParser.INSTANCE.convert("[{ \"regex\" : \"[\", \"tracing\" : \"disabled\" }]");
            fail("json is invalid, it should throw exceptions");
        } catch (InvalidConfigException e) {
            System.out.println("Found expected exception. Message: " + e.getMessage());
        }
    }
}
