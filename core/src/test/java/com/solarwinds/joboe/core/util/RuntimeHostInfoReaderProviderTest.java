package com.solarwinds.joboe.core.util;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RuntimeHostInfoReaderProviderTest {
  private final RuntimeHostInfoReaderProvider tested = new RuntimeHostInfoReaderProvider();

  @Test
  void returnServerHostInfoReader() {
    assertTrue(tested.getHostInfoReader() instanceof ServerHostInfoReader);
  }
}
