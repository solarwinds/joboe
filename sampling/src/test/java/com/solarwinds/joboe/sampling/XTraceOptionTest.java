package com.solarwinds.joboe.sampling;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class XTraceOptionTest {
  @Test
  public void testFromKey() {
    assertEquals(
        XTraceOption.TRIGGER_TRACE, XTraceOption.fromKey(XTraceOption.TRIGGER_TRACE.getKey()));
    assertTrue(XTraceOption.fromKey(XTraceOption.CUSTOM_KV_PREFIX + "abc").isCustomKv());
    assertNull(XTraceOption.fromKey("unknown"));
    assertNull(XTraceOption.fromKey("trigger trace"));
  }
}
