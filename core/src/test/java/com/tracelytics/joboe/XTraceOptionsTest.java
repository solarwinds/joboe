package com.tracelytics.joboe;

import com.tracelytics.joboe.XTraceOptions.HmacSignatureAuthenticator;
import org.junit.jupiter.api.Test;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class XTraceOptionsTest {


    @Test
    public void testGetXTraceOptions() throws Exception {
        assertNull(XTraceOptions.getXTraceOptions(null, null));

        String swKeys = "lo:se";
        XTraceOptions options = XTraceOptions.getXTraceOptions(
                XTraceOption.TRIGGER_TRACE.getKey() + XTraceOptions.ENTRY_SEPARATOR +
                XTraceOption.SW_KEYS.getKey() + XTraceOptions.KEY_VALUE_SEPARATOR + swKeys + XTraceOptions.ENTRY_SEPARATOR +
                XTraceOption.CUSTOM_KV_PREFIX + "tag1" + XTraceOptions.KEY_VALUE_SEPARATOR + "v1" + XTraceOptions.ENTRY_SEPARATOR +
                XTraceOption.CUSTOM_KV_PREFIX + "tag2" + XTraceOptions.KEY_VALUE_SEPARATOR + "v2",
                null);
        assertEquals(XTraceOptions.AuthenticationStatus.NOT_AUTHENTICATED, options.getAuthenticationStatus());

        HashMap<XTraceOption<String>, String> expectedCustomKvs = new HashMap<XTraceOption<String>, String>();
        expectedCustomKvs.put((XTraceOption<String>) XTraceOption.fromKey(XTraceOption.CUSTOM_KV_PREFIX + "tag1"), "v1");
        expectedCustomKvs.put((XTraceOption<String>) XTraceOption.fromKey(XTraceOption.CUSTOM_KV_PREFIX + "tag2"), "v2");

        assertEquals(swKeys, options.getOptionValue(XTraceOption.SW_KEYS));
        assertEquals(Boolean.TRUE, options.getOptionValue(XTraceOption.TRIGGER_TRACE));
        assertEquals(expectedCustomKvs, options.getCustomKvs());
        assertEquals(Collections.EMPTY_LIST, options.getExceptions());

        //no trigger-trace option
        options = XTraceOptions.getXTraceOptions(
                        XTraceOption.SW_KEYS.getKey() + XTraceOptions.KEY_VALUE_SEPARATOR + swKeys + XTraceOptions.ENTRY_SEPARATOR +
                        XTraceOption.CUSTOM_KV_PREFIX + "tag1" + XTraceOptions.KEY_VALUE_SEPARATOR + "v1" + XTraceOptions.ENTRY_SEPARATOR +
                        XTraceOption.CUSTOM_KV_PREFIX + "tag2" + XTraceOptions.KEY_VALUE_SEPARATOR + "v2",
                null);
        assertEquals(XTraceOptions.AuthenticationStatus.NOT_AUTHENTICATED, options.getAuthenticationStatus());
        assertEquals(Collections.EMPTY_LIST, options.getExceptions());
        assertEquals(swKeys, options.getOptionValue(XTraceOption.SW_KEYS));
        assertEquals(expectedCustomKvs, options.getCustomKvs());
    }

    @Test
    public void testFormatting() {
        XTraceOptions options;
        String swKeys = "lo:se";
        //leading and trailing whitespace
        options = XTraceOptions.getXTraceOptions(
                "      " + XTraceOption.TRIGGER_TRACE.getKey() + XTraceOptions.ENTRY_SEPARATOR +
                        XTraceOption.SW_KEYS.getKey() + XTraceOptions.KEY_VALUE_SEPARATOR + swKeys + "      ",
                null);
        assertEquals(swKeys, options.getOptionValue(XTraceOption.SW_KEYS));
        assertEquals(Boolean.TRUE, options.getOptionValue(XTraceOption.TRIGGER_TRACE));

        //space in between kv pairs are trimmed
        //leading and trailing whitespace
        options = XTraceOptions.getXTraceOptions(
                XTraceOption.TRIGGER_TRACE.getKey() + " " + XTraceOptions.ENTRY_SEPARATOR + " " +
                        XTraceOption.SW_KEYS.getKey() + " " + XTraceOptions.KEY_VALUE_SEPARATOR + " " + swKeys,
                null);
        assertEquals(swKeys, options.getOptionValue(XTraceOption.SW_KEYS));
        assertEquals(Boolean.TRUE, options.getOptionValue(XTraceOption.TRIGGER_TRACE));

        //space in key is considered invalid
        options = XTraceOptions.getXTraceOptions("trigger trace", null);
        assertEquals(Boolean.FALSE, options.getOptionValue(XTraceOption.TRIGGER_TRACE));
        assertEquals("trigger trace", ((XTraceOptions.UnknownXTraceOptionException)options.getExceptions().get(0)).getInvalidOptionKey());

        //key/value separator (=) in value is okay
        String customKey = XTraceOption.CUSTOM_KV_PREFIX + "1";
        String customValue = "foo" + XTraceOptions.KEY_VALUE_SEPARATOR + "5";
        options = XTraceOptions.getXTraceOptions(customKey + XTraceOptions.KEY_VALUE_SEPARATOR + customValue , null);
        assertEquals(0, options.getExceptions().size());
        assertEquals(1, options.getCustomKvs().size());
        assertEquals(customKey, options.getCustomKvs().keySet().iterator().next().getKey());
        assertEquals(customValue, options.getCustomKvs().values().iterator().next());
    }

    @Test
    public void testDuplicatedOption() {
        XTraceOptions options = XTraceOptions.getXTraceOptions(
                XTraceOption.SW_KEYS.getKey() + XTraceOptions.KEY_VALUE_SEPARATOR + "p1" + XTraceOptions.ENTRY_SEPARATOR +
                        XTraceOption.SW_KEYS.getKey() + XTraceOptions.KEY_VALUE_SEPARATOR + "p2" + XTraceOptions.ENTRY_SEPARATOR +
                        XTraceOption.CUSTOM_KV_PREFIX + "tag1" + XTraceOptions.KEY_VALUE_SEPARATOR + "v1" + XTraceOptions.ENTRY_SEPARATOR +
                        XTraceOption.CUSTOM_KV_PREFIX + "tag1" + XTraceOptions.KEY_VALUE_SEPARATOR + "v2",
                null);
        HashMap<XTraceOption<String>, String> expectedCustomKvs = new HashMap<XTraceOption<String>, String>();
        expectedCustomKvs.put((XTraceOption<String>) XTraceOption.fromKey(XTraceOption.CUSTOM_KV_PREFIX + "tag1"), "v1"); //take the first value only

        assertEquals("p1", options.getOptionValue(XTraceOption.SW_KEYS));
        assertEquals(expectedCustomKvs, options.getCustomKvs());
    }

    @Test
    public void testGetXTraceOptionsExceptions() throws Exception {
        XTraceOptions options = XTraceOptions.getXTraceOptions(
                XTraceOption.TRIGGER_TRACE.getKey() + XTraceOptions.ENTRY_SEPARATOR +
                        "unknown-tag1" + XTraceOptions.KEY_VALUE_SEPARATOR + "v1" + XTraceOptions.ENTRY_SEPARATOR +
                        "unknown-tag2",
                null);

        assertEquals(Boolean.TRUE, options.getOptionValue(XTraceOption.TRIGGER_TRACE));
        assertEquals(2, options.getExceptions().size());
        assertEquals("unknown-tag1", ((XTraceOptions.UnknownXTraceOptionException)options.getExceptions().get(0)).getInvalidOptionKey());
        assertEquals("unknown-tag2", ((XTraceOptions.UnknownXTraceOptionException)options.getExceptions().get(1)).getInvalidOptionKey());


        //test invalid format
        options = XTraceOptions.getXTraceOptions(
                XTraceOption.TRIGGER_TRACE.getKey() + XTraceOptions.KEY_VALUE_SEPARATOR + "1" +  XTraceOptions.ENTRY_SEPARATOR +
                XTraceOption.CUSTOM_KV_PREFIX + "1", null); //trigger trace should not have value, custom kv should have a value
        assertEquals(Boolean.FALSE, options.getOptionValue(XTraceOption.TRIGGER_TRACE));
        assertEquals(XTraceOption.TRIGGER_TRACE.getKey(), ((XTraceOptions.InvalidFormatXTraceOptionException)options.getExceptions().get(0)).getInvalidOptionKey());
        assertEquals(XTraceOption.CUSTOM_KV_PREFIX + "1", ((XTraceOptions.InvalidFormatXTraceOptionException)options.getExceptions().get(1)).getInvalidOptionKey());

        //test invalid value
        options = XTraceOptions.getXTraceOptions(
                XTraceOption.TS.getKey() + XTraceOptions.KEY_VALUE_SEPARATOR + "abc", null); //ts should be a long
        assertEquals(XTraceOption.TS.getKey(), ((XTraceOptions.InvalidValueXTraceOptionException)options.getExceptions().get(0)).getInvalidOptionKey());

        //parse some options either though others are bad
        options = XTraceOptions.getXTraceOptions("trigger-trace;custom-foo=' bar;bar' ;custom-bar=foo", null);
        assertEquals(Boolean.TRUE, options.getOptionValue(XTraceOption.TRIGGER_TRACE));
        assertEquals(2, options.getCustomKvs().size());
        Iterator<XTraceOption<String>> customKeyIterator = options.getCustomKvs().keySet().iterator();
        Iterator<String> customValueIterator = options.getCustomKvs().values().iterator();
        assertEquals("custom-foo", customKeyIterator.next().getKey());
        assertEquals("' bar", customValueIterator.next());

        assertEquals("custom-bar", customKeyIterator.next().getKey());
        assertEquals("foo", customValueIterator.next());

        assertEquals("bar'", ((XTraceOptions.UnknownXTraceOptionException)options.getExceptions().get(0)).getInvalidOptionKey());

        options = XTraceOptions.getXTraceOptions(";trigger-trace;custom-something=value_thing;sw-keys=02973r70:9wqj21,0d9j1;1;2;=custom-key=val?;=", null);
        assertEquals(Boolean.TRUE, options.getOptionValue(XTraceOption.TRIGGER_TRACE));
        assertEquals("02973r70:9wqj21,0d9j1", options.getOptionValue(XTraceOption.SW_KEYS));
        assertEquals(1, options.getCustomKvs().size());
        customKeyIterator = options.getCustomKvs().keySet().iterator();
        customValueIterator = options.getCustomKvs().values().iterator();
        assertEquals("custom-something", customKeyIterator.next().getKey());
        assertEquals("value_thing", customValueIterator.next());
        assertEquals(2, options.getExceptions().size()); //should only flag exception for 1 and 2, the last two entry starts with '=' will be ignored
        assertEquals("1", ((XTraceOptions.UnknownXTraceOptionException) options.getExceptions().get(0)).getInvalidOptionKey());
        assertEquals("2", ((XTraceOptions.UnknownXTraceOptionException) options.getExceptions().get(1)).getInvalidOptionKey());

        //skip sequel ;
        options = XTraceOptions.getXTraceOptions("custom-something=value_thing;sw-keys=02973r70;;;;custom-key=val", null);
        assertEquals("02973r70", options.getOptionValue(XTraceOption.SW_KEYS));
        assertEquals(2, options.getCustomKvs().size());
        customKeyIterator = options.getCustomKvs().keySet().iterator();
        customValueIterator = options.getCustomKvs().values().iterator();
        assertEquals("custom-something", customKeyIterator.next().getKey());
        assertEquals("value_thing", customValueIterator.next());
        assertEquals("custom-key", customKeyIterator.next().getKey());
        assertEquals("val", customValueIterator.next());

        //case sensitive
        options = XTraceOptions.getXTraceOptions("Trigger-Trace;Custom-something=value_thing", null);
        assertEquals(Boolean.FALSE, options.getOptionValue(XTraceOption.TRIGGER_TRACE));
        assertEquals(2, options.getExceptions().size());
        assertEquals("Trigger-Trace", ((XTraceOptions.UnknownXTraceOptionException)options.getExceptions().get(0)).getInvalidOptionKey());
        assertEquals("Custom-something", ((XTraceOptions.UnknownXTraceOptionException)options.getExceptions().get(1)).getInvalidOptionKey());

        //no X-Trace-Options but has signature
        options = XTraceOptions.getXTraceOptions(null, "abc");
        assertEquals(null, options);

    }

    @Test
    public void testHmacAuthenticator() throws Exception {
        HmacSignatureAuthenticator authenticator = new XTraceOptions.HmacSignatureAuthenticator("8mZ98ZnZhhggcsUmdMbS".getBytes(Charset.forName("US-ASCII")));

        assertEquals(true, authenticator.authenticate("trigger-trace;sw-keys=lo:se,check-id:123;ts=1564597681", "26e33ce58c52afc507c5c1e9feff4ac5562c9e1c"));
        assertEquals(false, authenticator.authenticate("trigger-trace;sw-keys=lo:se,check-id:123;ts=1564597681", "2c1c398c3e6be898f47f74bf74f035903b48baaa"));
    }

    @Test
    public void testAuthenticate() {
        HmacSignatureAuthenticator authenticator = new XTraceOptions.HmacSignatureAuthenticator("8mZ98ZnZhhggcsUmdMbS".getBytes(Charset.forName("US-ASCII")));

        //missing ts
        assertEquals(XTraceOptions.AuthenticationStatus.failure("bad-timestamp"), XTraceOptions.authenticate("trigger-trace;sw-keys=lo:se,check-id:123", null, "2c1c398c3e6be898f47f74bf74f035903b48b59c", authenticator));

        long outOfRangeTimestamp = System.currentTimeMillis() / 1000 - (XTraceOptions.TIMESTAMP_MAX_DELTA + 1);
        //timestamp out of range
        assertEquals(XTraceOptions.AuthenticationStatus.failure("bad-timestamp"), XTraceOptions.authenticate("trigger-trace;sw-keys=lo:se,check-id:123;ts=" + outOfRangeTimestamp, outOfRangeTimestamp, "2c1c398c3e6be898f47f74bf74f035903b48b59c", authenticator));

        //no signature
        assertEquals(XTraceOptions.AuthenticationStatus.NOT_AUTHENTICATED, XTraceOptions.authenticate("trigger-trace;sw-keys=lo:se,check-id:123", null, null, authenticator));

        //valid signature - using a mock up authenticator here to bypass the signature check - which is verify in testHmacAuthenticator
        long goodTimestamp = System.currentTimeMillis() / 1000;
        assertEquals(XTraceOptions.AuthenticationStatus.OK, XTraceOptions.authenticate("trigger-trace;sw-keys=lo:se,check-id:123;ts=" + goodTimestamp, goodTimestamp, "2c1c398c3e6be898f47f74bf74f035903b48b59c", ((optionsString, signature) -> true)));

        //authenticator not ready
        assertEquals(XTraceOptions.AuthenticationStatus.failure("authenticator-unavailable"), XTraceOptions.authenticate("trigger-trace;sw-keys=lo:se,check-id:123;ts=" + goodTimestamp, goodTimestamp, "2c1c398c3e6be898f47f74bf74f035903b48b59c", null));
    }
}
