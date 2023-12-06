package com.solarwinds.joboe.span.impl;

import com.solarwinds.joboe.TraceDecisionUtil;
import com.solarwinds.joboe.TracingMode;
import com.solarwinds.joboe.settings.SettingsArg;
import com.solarwinds.joboe.settings.SettingsManager;
import com.solarwinds.joboe.settings.SimpleSettingsFetcher;
import com.solarwinds.joboe.settings.TestSettingsReader;
import com.solarwinds.joboe.settings.TestSettingsReader.SettingsMockupBuilder;
import com.solarwinds.joboe.span.impl.Span.SpanProperty;
import com.solarwinds.joboe.span.impl.Span.TraceProperty;
import com.solarwinds.util.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TransactionNameManagerTest {
    protected static final TestSettingsReader testSettingsReader = TestUtils.initSettingsReader();
    
    @BeforeEach
    protected void setUp() throws Exception {
        testSettingsReader.reset();
        testSettingsReader.put(TestUtils.getDefaultSettings());
    
        ScopeManager.INSTANCE.removeAllScopes();
        TransactionNameManager.reset();
    }
    
    @AfterEach
    protected void tearDown() throws Exception {
        testSettingsReader.reset();
    
        TransactionNameManager.urlTransactionNameCache.invalidateAll();
        ScopeManager.INSTANCE.removeAllScopes();
        TransactionNameManager.reset();
    }

    @Test
    public void testTransactionNameLimit() {
        TransactionNameManager.clearTransactionNames();
        
        Span span;
        for (int i = 0 ; i < TransactionNameManager.DEFAULT_MAX_NAME_COUNT; i ++) {
            span = Tracer.INSTANCE.buildSpan("").start();
            span.setTracePropertyValue(TraceProperty.ACTION, "a" + i);
            span.setTracePropertyValue(TraceProperty.CONTROLLER, "c");
            assertEquals("c.a" + i, TransactionNameManager.getTransactionName(span));
            assertFalse(TransactionNameManager.isLimitExceeded());
            span.finish();
        }
        
        span = Tracer.INSTANCE.buildSpan("").start();
        span.setTracePropertyValue(TraceProperty.ACTION, "a" + 0);
        span.setTracePropertyValue(TraceProperty.CONTROLLER, "c");
        assertEquals("c.a0", TransactionNameManager.getTransactionName(span)); //OK, as this name already exists
        assertFalse(TransactionNameManager.isLimitExceeded());
        span.finish();
        
        span = Tracer.INSTANCE.buildSpan("").start();
        span.setTracePropertyValue(TraceProperty.ACTION, "a");
        span.setTracePropertyValue(TraceProperty.CONTROLLER, "c");
        assertEquals(TransactionNameManager.OVER_LIMIT_TRANSACTION_NAME, TransactionNameManager.getTransactionName(span)); //over the limit
        assertTrue(TransactionNameManager.isLimitExceeded());
        span.finish();
        
        //simulate a limit change (higher)
        SimpleSettingsFetcher fetcher = (SimpleSettingsFetcher) SettingsManager.getFetcher();
        testSettingsReader.put(new SettingsMockupBuilder().withFlags(TracingMode.ALWAYS).withSampleRate(TraceDecisionUtil.SAMPLE_RESOLUTION).withSettingsArg(SettingsArg.MAX_TRANSACTIONS, TransactionNameManager.DEFAULT_MAX_NAME_COUNT + 1).build());
        
        //now it should be okay to add this name
        span = Tracer.INSTANCE.buildSpan("").start();
        span.setTracePropertyValue(TraceProperty.ACTION, "a");
        span.setTracePropertyValue(TraceProperty.CONTROLLER, "c");
        assertEquals("c.a", TransactionNameManager.getTransactionName(span));
        assertFalse(TransactionNameManager.isLimitExceeded());
        span.finish();
        
        //add one more and it should over the limit again
        span = Tracer.INSTANCE.buildSpan("").start();
        span.setTracePropertyValue(TraceProperty.ACTION, "aa");
        span.setTracePropertyValue(TraceProperty.CONTROLLER, "c");
        assertEquals(TransactionNameManager.OVER_LIMIT_TRANSACTION_NAME, TransactionNameManager.getTransactionName(span));
        assertTrue(TransactionNameManager.isLimitExceeded());
        span.finish();
        
        //if args override is no longer present, then revert back to default
        testSettingsReader.put(new SettingsMockupBuilder().withFlags(TracingMode.ALWAYS).withSampleRate(TraceDecisionUtil.SAMPLE_RESOLUTION).build());
        
        TransactionNameManager.clearTransactionNames();
        for (int i = 0 ; i < TransactionNameManager.DEFAULT_MAX_NAME_COUNT; i ++) {
            span = Tracer.INSTANCE.buildSpan("").start();
            span.setTracePropertyValue(TraceProperty.ACTION, "a" + i);
            span.setTracePropertyValue(TraceProperty.CONTROLLER, "c");
            assertEquals("c.a" + i, TransactionNameManager.getTransactionName(span));
            assertFalse(TransactionNameManager.isLimitExceeded());
            span.finish();
        }
        
        span = Tracer.INSTANCE.buildSpan("").start();
        span.setTracePropertyValue(TraceProperty.ACTION, "a");
        span.setTracePropertyValue(TraceProperty.CONTROLLER, "c");
        assertEquals(TransactionNameManager.OVER_LIMIT_TRANSACTION_NAME, TransactionNameManager.getTransactionName(span)); //over the limit
        assertTrue(TransactionNameManager.isLimitExceeded());
        span.finish();
    }

    @Test
    public void testUnknownTransactionName() {
        //no URL nor controller/action, should return unknown
        Span span = Tracer.INSTANCE.buildSpan("").start();
        assertEquals(TransactionNameManager.UNKNOWN_TRANSACTION_NAME, TransactionNameManager.getTransactionName(span)); 
        span.finish();
    }

    @Test
    public void testGetTransactionNameByPattern() throws Exception {
        String host = "localhost:8080";
        String url = "/1/2/3";
        
        String pattern;
        
        pattern = "host,p1";
        assertEquals("localhost.1", TransactionNameManager.buildTransactionNameByUrlAndPattern(host, url, TransactionNameManager.parseTransactionNamePattern(pattern), false, "."));
        
        pattern = "host,p1,p2";
        assertEquals("localhost.1.2", TransactionNameManager.buildTransactionNameByUrlAndPattern(host, url, TransactionNameManager.parseTransactionNamePattern(pattern), false, "."));
        
        pattern = "host,p2,p3";
        assertEquals("localhost.2.3", TransactionNameManager.buildTransactionNameByUrlAndPattern(host, url, TransactionNameManager.parseTransactionNamePattern(pattern), false, "."));
        
        pattern = "host,p3,p4";
        assertEquals("localhost.3", TransactionNameManager.buildTransactionNameByUrlAndPattern(host, url, TransactionNameManager.parseTransactionNamePattern(pattern), false, "."));
        
        pattern = "host,p4";
        assertEquals("localhost", TransactionNameManager.buildTransactionNameByUrlAndPattern(host, url, TransactionNameManager.parseTransactionNamePattern(pattern), false, "."));
        
        pattern = "p5";
        assertEquals("", TransactionNameManager.buildTransactionNameByUrlAndPattern(host, url, TransactionNameManager.parseTransactionNamePattern(pattern), false, "."));
    }

    @Test
    public void testGetTransactionNamePrecedence()  throws Exception {
        Span span = Tracer.INSTANCE.buildSpan("").start();
        span.setTag("Status", 200);
        span.setTag("HTTP-Host", "localhost:8080");
        span.setTag("URL", "/1/2/3?abc=123");
        
        assertEquals("/1/2", TransactionNameManager.getTransactionName(span)); //using the default URL mapping
        
        //now add action/controller
        span.setTracePropertyValue(TraceProperty.ACTION, "a");
        span.setTracePropertyValue(TraceProperty.CONTROLLER, "c");
        
        assertEquals("c.a", TransactionNameManager.getTransactionName(span));
        
        //now add custom pattern
        TransactionNameManager.customTransactionNamePattern = new String[] { "host", "p2", "p3" };
        TransactionNameManager.urlTransactionNameCache.invalidateAll();
        assertEquals("localhost.2.3", TransactionNameManager.getTransactionName(span)); //should not contain query param
        
        //now add a transaction name from out-of-the-box instrumentation
        span.setTracePropertyValue(TraceProperty.TRANSACTION_NAME, "out-of-the-box-transaction");
        assertEquals("out-of-the-box-transaction", TransactionNameManager.getTransactionName(span)); //now it should use custom transaction name from out-of-the-box instrumentation
        
        //now add custom transaction name
        span.setTracePropertyValue(TraceProperty.CUSTOM_TRANSACTION_NAME, "my-transaction");
        assertEquals("my-transaction", TransactionNameManager.getTransactionName(span)); //now it should use custom transaction name
        TransactionNameManager.customTransactionNamePattern = null; //reset
        
        //a span with no URL nor controller/action
        span = Tracer.INSTANCE.buildSpan("").start();
        assertEquals(TransactionNameManager.UNKNOWN_TRANSACTION_NAME, TransactionNameManager.getTransactionName(span)); //cannot form any transaction name
        
        //add controller/action
        span.setTracePropertyValue(TraceProperty.ACTION, "a");
        span.setTracePropertyValue(TraceProperty.CONTROLLER, "c");
        assertEquals("c.a", TransactionNameManager.getTransactionName(span));
        
        //add custom pattern - should not alter the result as there's no URL, so controller/action should be used 
        TransactionNameManager.customTransactionNamePattern = new String[] { "host", "p2", "p3" };
        assertEquals("c.a", TransactionNameManager.getTransactionName(span));
        TransactionNameManager.customTransactionNamePattern = null; //reset
        
        span.finish();
    }

    @Test
    public void testGetTransactionNameWithDomain()  throws Exception { 
        boolean originalDomainPrefixedTransactionName = TransactionNameManager.domainPrefixedTransactionName;
        TransactionNameManager.domainPrefixedTransactionName = true;
        
        Span span;
        
        //action controller
        span = Tracer.INSTANCE.buildSpan("").start();
        span.setTracePropertyValue(TraceProperty.ACTION, "a");
        span.setTracePropertyValue(TraceProperty.CONTROLLER, "c");
        span.setTag("HTTP-Host", "my.host");
        assertEquals("my.host/c.a", TransactionNameManager.getTransactionName(span));
        span.finish();
        
        //url
        span = Tracer.INSTANCE.buildSpan("").start();
        span.setTag("URL", "/1/2/3?abc=123");
        span.setTag("HTTP-Host", "my.host");
        assertEquals("my.host/1/2", TransactionNameManager.getTransactionName(span));
        span.finish();
        
        //out-of-the-box transaction namne
        span = Tracer.INSTANCE.buildSpan("").start();
        span.setTracePropertyValue(TraceProperty.TRANSACTION_NAME, "my-transaction");
        span.setTag("HTTP-Host", "my.host");
        assertEquals("my.host/my-transaction", TransactionNameManager.getTransactionName(span));
        span.finish();
        
        
        //no HTTP-Host tag
        span = Tracer.INSTANCE.buildSpan("").start();
        span.setTracePropertyValue(TraceProperty.TRANSACTION_NAME, "my-transaction");
        assertEquals("my-transaction", TransactionNameManager.getTransactionName(span));
        span.finish();
        
        //HTTP-Host with port number
        span = Tracer.INSTANCE.buildSpan("").start();
        span.setTracePropertyValue(TraceProperty.TRANSACTION_NAME, "my-transaction");
        span.setTag("HTTP-Host", "my.host:8080");
        assertEquals("my.host:8080/my-transaction", TransactionNameManager.getTransactionName(span));
        span.finish();
        
        //unknown should not have domain name prefix
        span = Tracer.INSTANCE.buildSpan("").start();
        span.setTag("HTTP-Host", "my.host");
        assertEquals("unknown", TransactionNameManager.getTransactionName(span));
        span.finish();
        
        //other should not have domain name prefix
        TransactionNameManager.reset();
        for (int i = 0 ; i < TransactionNameManager.DEFAULT_MAX_NAME_COUNT; i ++) {
            span = Tracer.INSTANCE.buildSpan("").start();
            span.setTracePropertyValue(TraceProperty.ACTION, "a" + i);
            span.setTracePropertyValue(TraceProperty.CONTROLLER, "c");
            span.setTag("HTTP-Host", "my.host");
            assertEquals("my.host/c.a" + i, TransactionNameManager.getTransactionName(span));
            assertFalse(TransactionNameManager.isLimitExceeded());
            span.finish();
        }
        
        //hitting the limit, returning other
        span = Tracer.INSTANCE.buildSpan("").start();
        span.setTracePropertyValue(TraceProperty.ACTION, "aa");
        span.setTracePropertyValue(TraceProperty.CONTROLLER, "c");
        span.setTag("HTTP-Host", "my.host");
        assertEquals(TransactionNameManager.OVER_LIMIT_TRANSACTION_NAME, TransactionNameManager.getTransactionName(span));
        assertTrue(TransactionNameManager.isLimitExceeded());
        
        //different domain prefixs should be counted against transaction name
        TransactionNameManager.reset();
        for (int i = 0 ; i < TransactionNameManager.DEFAULT_MAX_NAME_COUNT; i ++) {
            span = Tracer.INSTANCE.buildSpan("").start();
            span.setTracePropertyValue(TraceProperty.ACTION, "a");
            span.setTracePropertyValue(TraceProperty.CONTROLLER, "c");
            span.setTag("HTTP-Host", "my.host" + i);
            assertEquals("my.host" + i + "/c.a", TransactionNameManager.getTransactionName(span));
            assertFalse(TransactionNameManager.isLimitExceeded());
            span.finish();
        }
        
        //hitting the limit, returning other
        span = Tracer.INSTANCE.buildSpan("").start();
        span.setTracePropertyValue(TraceProperty.ACTION, "a");
        span.setTracePropertyValue(TraceProperty.CONTROLLER, "c");
        span.setTag("HTTP-Host", "some.other.host");
        assertEquals(TransactionNameManager.OVER_LIMIT_TRANSACTION_NAME, TransactionNameManager.getTransactionName(span));
        assertTrue(TransactionNameManager.isLimitExceeded());
        
        //reset the flag
        TransactionNameManager.domainPrefixedTransactionName = originalDomainPrefixedTransactionName;
    }

    @Test
    public void testTransformTransactionName() {
        assertEquals("valid-name", TransactionNameManager.transformTransactionName("valid-name"));
        
        //test long string that JUST make the cut
        String longString = generateLongString(TransactionNameManager.MAX_TRANSACTION_NAME_LENGTH);
        assertEquals(longString, TransactionNameManager.transformTransactionName(longString));
        
        //test long string that doesn't make the cut
        longString = generateLongString(TransactionNameManager.MAX_TRANSACTION_NAME_LENGTH + 1);
        assertTrue(!longString.equals(TransactionNameManager.transformTransactionName(longString))); //should not be equal as it should be trimmed
        assertEquals(generateLongString(TransactionNameManager.MAX_TRANSACTION_NAME_LENGTH - TransactionNameManager.TRANSACTION_NAME_ELLIPSIS.length()) + TransactionNameManager.TRANSACTION_NAME_ELLIPSIS, TransactionNameManager.transformTransactionName(longString));
        
        assertEquals("abc", TransactionNameManager.transformTransactionName("ABC")); //convert to lower case
        assertEquals("some space is ok", TransactionNameManager.transformTransactionName("some space is ok"));
        assertEquals("  ", TransactionNameManager.transformTransactionName("  "));
        assertEquals(" ", TransactionNameManager.transformTransactionName("")); //should at least have 1 space
        assertEquals("some_invalid_character_", TransactionNameManager.transformTransactionName("some*invalid(character)"));
        assertEquals("-.:_\\/?", TransactionNameManager.transformTransactionName("-.:_\\/?"));
        
    }

    @Test
    public void testGetSdkDefaultTransactionName() {
        //test regular non-sdk trace. it should still give unknown if not enough info is provided
        Span span = Tracer.INSTANCE.buildSpan("ootb-span").start();
        assertEquals(TransactionNameManager.UNKNOWN_TRANSACTION_NAME, TransactionNameManager.getTransactionName(span));
        span.finish();
        
        //a sdk trace, with no explicit transaction name, should use default sdk transaction name custom-<top span name>
        span = Tracer.INSTANCE.buildSpan("sdk-span").start();
        span.setSpanPropertyValue(SpanProperty.IS_SDK, true);
        assertEquals(TransactionNameManager.DEFAULT_SDK_TRANSACTION_NAME_PREFIX + "sdk-span", TransactionNameManager.getTransactionName(span));
        
      //a sdk trace, with explicit transaction name, should use the name set
        span.setTracePropertyValue(TraceProperty.CUSTOM_TRANSACTION_NAME, "custom-name");
        assertEquals("custom-name", TransactionNameManager.getTransactionName(span));
        span.finish();
    }

    private static String generateLongString(int length) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0 ; i < length ; i++) {
            builder.append('a');
        }
        
        return builder.toString();
    }
}