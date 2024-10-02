package com.tracelytics.agent.config;

import com.tracelytics.joboe.SampleRateSource;
import com.tracelytics.joboe.config.InvalidConfigException;
import com.tracelytics.joboe.config.TraceConfigs;
import com.tracelytics.joboe.rpc.Settings;
import org.junit.Test;
import static org.junit.Assert.*;

public class UrlSampleRateConfigParserTest {
    @Test
    public void testValidConfigs() throws InvalidConfigException {
        //test the sampleRate/tracingMode combo
        TraceConfigs configs;
        configs = UrlSampleRateConfigParser.INSTANCE.convert("[{ \".*\" : { \"sampleRate\" : 1000000, \"tracingMode\" : \"always\" } }]");

        assertEquals(1000000, configs.getTraceConfig("abc").getSampleRate());
        assertEquals(SampleRateSource.FILE, configs.getTraceConfig("abc").getSampleRateSource());
        assertEquals(true, configs.getTraceConfig("abc").hasSampleStartFlag());
        assertEquals(true, configs.getTraceConfig("abc").hasSampleTriggerTraceFlag());
        assertEquals(true, configs.getTraceConfig("abc").hasSampleThroughAlwaysFlag());

        configs = UrlSampleRateConfigParser.INSTANCE.convert("[{ \".*\" : { \"sampleRate\" : 0, \"tracingMode\" : \"never\" } }]");

        assertEquals(0, configs.getTraceConfig("abc").getSampleRate());
        assertEquals(false, configs.getTraceConfig("abc").hasSampleStartFlag());
        assertEquals(false, configs.getTraceConfig("abc").hasSampleTriggerTraceFlag());
        assertEquals(false, configs.getTraceConfig("abc").hasSampleThroughAlwaysFlag());

        configs = UrlSampleRateConfigParser.INSTANCE.convert("[{ \".*\" : { \"tracingMode\" : \"never\" } }]"); //sampleRate is optional for "never"

        assertEquals(0, configs.getTraceConfig("abc").getSampleRate());
        assertEquals(false, configs.getTraceConfig("abc").hasSampleStartFlag());
        assertEquals(false, configs.getTraceConfig("abc").hasSampleTriggerTraceFlag());
        assertEquals(false, configs.getTraceConfig("abc").hasSampleThroughAlwaysFlag());

        configs = UrlSampleRateConfigParser.INSTANCE.convert("[{ \".*\" : { \"sampleRate\" : 100 } }]"); //if tracingMode is not defined then it's default to "always"

        assertEquals(100, configs.getTraceConfig("abc").getSampleRate());
        assertEquals(true, configs.getTraceConfig("abc").hasSampleStartFlag());
        assertEquals(true, configs.getTraceConfig("abc").hasSampleTriggerTraceFlag());
        assertEquals(true, configs.getTraceConfig("abc").hasSampleThroughAlwaysFlag());


        //test on pattern matching
        configs = UrlSampleRateConfigParser.INSTANCE.convert("[{ \".*\\\\.html\" : { \"sampleRate\" : 1000000, \"tracingMode\" : \"always\" } }]");
        assertTrue(configs.getTraceConfig("abc.html") != null);
        assertTrue(configs.getTraceConfig("abc.HTML") != null); //case insensitive
        assertTrue(configs.getTraceConfig("abc.HTML?abc") == null); //trailing contents do not match
        assertTrue(configs.getTraceConfig("abc.htm") == null);
    }


    @Test
    public void testInvalidConfigs() {
        try {
            UrlSampleRateConfigParser.INSTANCE.convert("[{ \".*\" : { \"sampleRate\" : 1000000, \"tracingMode\" : \"always\" } }"); //invalid json, missing closing ]
            fail("Expect " + InvalidConfigException.class.getName() + " but found none");
        } catch (InvalidConfigException e) {
        }

        try {
            UrlSampleRateConfigParser.INSTANCE.convert("{ \".*\" : { \"sampleRate\" : 1000000, \"tracingMode\" : \"always\" } }"); //invalid, should be an array
            fail("Expect " + InvalidConfigException.class.getName() + " but found none");
        } catch (InvalidConfigException e) {
        }

        try {
            UrlSampleRateConfigParser.INSTANCE.convert("[{ \".*\" : { \"rate\" : 1000000, \"tracingMode\" : \"always\" } }]"); //unknown key : "rate"
            fail("Expect " + InvalidConfigException.class.getName() + " but found none");
        } catch (InvalidConfigException e) {
        }

        try {
            UrlSampleRateConfigParser.INSTANCE.convert("[{ \".*\" : { \"sampleRate\" : 1000000, \"tracingMode\" : \"yes\" } }]"); //invalid tracingMode
            fail("Expect " + InvalidConfigException.class.getName() + " but found none");
        } catch (InvalidConfigException e) {
        }

        try {
            UrlSampleRateConfigParser.INSTANCE.convert("[{ \".*\" : { } }]"); //must define either tracingMode and/or sampleRate
            fail("Expect " + InvalidConfigException.class.getName() + " but found none");
        } catch (InvalidConfigException e) {
        }

        try {
            UrlSampleRateConfigParser.INSTANCE.convert("[{ \".*\" : { \"tracingMode\" : \"always\" } }]"); //if tracingMode = always, then sampleRate should be defined
            fail("Expect " + InvalidConfigException.class.getName() + " but found none");
        } catch (InvalidConfigException e) {
        }

    }
}
