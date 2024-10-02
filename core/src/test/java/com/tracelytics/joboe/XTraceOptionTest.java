package com.tracelytics.joboe;

import junit.framework.TestCase;

public class XTraceOptionTest extends TestCase {
    public void testFromKey() {
        assertEquals(XTraceOption.TRIGGER_TRACE, XTraceOption.fromKey(XTraceOption.TRIGGER_TRACE.getKey()));
        assertEquals(true, XTraceOption.fromKey(XTraceOption.CUSTOM_KV_PREFIX + "abc").isCustomKv());
        assertEquals(null, XTraceOption.fromKey("unknown"));
        assertEquals(null, XTraceOption.fromKey("trigger trace"));
    }
}
