package com.tracelytics.agent.config;

import com.tracelytics.agent.config.UrlSampleRateConfigParser;
import com.tracelytics.joboe.JoboeTest;
import com.tracelytics.joboe.TraceConfig;
import com.tracelytics.joboe.config.InvalidConfigException;
import com.tracelytics.joboe.config.TraceConfigs;


public class TraceConfigsTest extends JoboeTest {
    
    public void testInvalidJson1()
        throws Exception {
        try {
            UrlSampleRateConfigParser.INSTANCE.convert("invalid json");
            fail("json is invalid, it should throw exceptions");
        } catch (InvalidConfigException e) {
            //expected
        }
    }
    
    public void testInvalidJson2() {
        //test invalid reg syntax
        try {
            UrlSampleRateConfigParser.INSTANCE.convert("[{ \"\\\" : { \"sampleRate\" : \"0\", \"tracingMode\" : \"never\" }}]");
            fail("json is invalid, it should throw exceptions");
        } catch (InvalidConfigException e) {
            //expected
        }
        
        //test invalid string type for sample rate
        try {
            UrlSampleRateConfigParser.INSTANCE.convert("[{ \".*\\\\.(png|jpg|jpeg|gif).*\" : { \"sampleRate\" : \"0\", \"tracingMode\" : \"never\" }}]");
            fail("json is invalid, it should throw exceptions");
        } catch (InvalidConfigException e) {
            //expected
        }
        
        //test invalid trace mode type
        try {
            UrlSampleRateConfigParser.INSTANCE.convert("[{ \".*\\\\.(png|jpg|jpeg|gif).*\" : { \"sampleRate\" : 0, \"tracingMode\" : 0 }}]");
            fail("json is invalid, it should throw exceptions");
        } catch (InvalidConfigException e) {
            //expected
        }
        
        //test invalid trace mode value
        try {
            UrlSampleRateConfigParser.INSTANCE.convert("[{ \".*\\\\.(png|jpg|jpeg|gif).*\" : { \"sampleRate\" : 0, \"tracingMode\" : \"invalid\" }}]");
            fail("json is invalid, it should throw exceptions");
        } catch (InvalidConfigException e) {
            //expected
        }
    }
    
    public void testValidJson1() throws Exception {
        TraceConfigs configs = UrlSampleRateConfigParser.INSTANCE.convert(
                "[{ \".*\\\\.(png|jpg|jpeg|gif).*\" : { \"sampleRate\" : 0, \"tracingMode\" : \"never\" }}, " +
        		" { \".*\" : { \"sampleRate\" : 1000000 } }]");
        
        TraceConfig config;
        config = configs.getTraceConfig("somedomain.com/static.png");
        assertNotNull(config);
        assertEquals(0, config.getSampleRate());
        assertFalse(config.hasOverrideFlag());
        assertFalse(config.hasSampleStartFlag());
        assertFalse(config.hasSampleThroughAlwaysFlag());
        assertFalse(config.hasSampleThroughFlag());
        assertFalse(config.isMetricsEnabled());
        
        
        
        config = configs.getTraceConfig("somedomain.com/static.PNG");
        assertNotNull(config);
        assertEquals(0, config.getSampleRate());
        assertFalse(config.hasOverrideFlag());
        assertFalse(config.hasSampleStartFlag());
        assertFalse(config.hasSampleThroughAlwaysFlag());
        assertFalse(config.hasSampleThroughFlag());
        assertFalse(config.isMetricsEnabled());
        
        config = configs.getTraceConfig("somedomain.com/something.html");
        assertNotNull(config);
        assertEquals(1000000, config.getSampleRate());
        assertFalse(config.hasOverrideFlag());
        assertTrue(config.hasSampleStartFlag());
        assertTrue(config.hasSampleThroughAlwaysFlag());
        assertFalse(config.hasSampleThroughFlag());
        assertTrue(config.isMetricsEnabled());
        
    }
    
    
    public void testValidJson2() throws Exception {
        TraceConfigs configs = UrlSampleRateConfigParser.INSTANCE.convert("[{ \".*\\\\.(png|jpg|jpeg|gif).*\" : { \"tracingMode\" : \"never\" }}]");
        
        TraceConfig config;
        
        config = configs.getTraceConfig("somedomain.com/static.png");
        assertNotNull(config);
        assertEquals(0, config.getSampleRate());
        assertFalse(config.hasOverrideFlag());
        assertFalse(config.hasSampleStartFlag());
        assertFalse(config.hasSampleThroughAlwaysFlag());
        assertFalse(config.hasSampleThroughFlag());
        assertFalse(config.isMetricsEnabled());
        
        config = configs.getTraceConfig("somedomain.com/static.PNG?param=true");
        assertNotNull(config);
        assertEquals(0, config.getSampleRate());
        assertFalse(config.hasOverrideFlag());
        assertFalse(config.hasSampleStartFlag());
        assertFalse(config.hasSampleThroughAlwaysFlag());
        assertFalse(config.hasSampleThroughFlag());
        assertFalse(config.isMetricsEnabled());
        
        
        config = configs.getTraceConfig("somedomain.com/something.html");
        assertNull(config);
        
    }
    
    //test precedence for url that matches multiple patterns
    public void testValidJson3() throws Exception {
        TraceConfigs configs = UrlSampleRateConfigParser.INSTANCE.convert("[{ \".*skipdomain\\\\.com/.*\" : { \"sampleRate\" : 0, \"tracingMode\" : \"never\" }}," +
        		                                                " { \".*\\\\.(html).*\" : { \"sampleRate\" : 1, \"tracingMode\" : \"always\" }}," +
        		                                                " { \".*\\\\.(png|jpg|jpeg|gif).*\" : { \"sampleRate\" : 0 }}"
        		                                                + "]");
        
        TraceConfig config;
        
        config = configs.getTraceConfig("www.notmatching.com/something.jsp");
        assertNull(config);
        
        config = configs.getTraceConfig("www.skipdomain1com/something.jsp");
        assertNull(config);
        
        config = configs.getTraceConfig("www.skipdomain.com.hk/something.jsp");
        assertNull(config);
        
        config = configs.getTraceConfig("www2.skipdomain.com/something.jsp");
        assertNotNull(config);
        assertEquals(0, config.getSampleRate());
        
        config = configs.getTraceConfig("www.match.com/something.html");
        assertNotNull(config);
        assertEquals(1, config.getSampleRate());
        assertTrue(config.isMetricsEnabled());
        
        
        config = configs.getTraceConfig("skipdomain.com/something.html"); //this url matches both patterns, but the tracingMode never one should be taken since it is defined first
        assertNotNull(config);
        assertEquals(0, config.getSampleRate());
        assertFalse(config.isMetricsEnabled());
        
        
        config = configs.getTraceConfig("www.match.com/pic.png"); //match sample rate = 0
        assertEquals(0, config.getSampleRate());
        assertTrue(config.isMetricsEnabled()); //should still report metrics as trace mode is implicitly always 
    }
}

