package com.solarwinds.joboe.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetSystemProperty;

class JavaRuntimeVersionCheckerTest {

  @Test
  @SetSystemProperty(key = "java.version", value = "1.8.0_250")
  void returnFalse() {
    boolean jdkVersionSupported = JavaRuntimeVersionChecker.isJdkVersionSupported();
    assertFalse(jdkVersionSupported);
  }

  @Test
  void returnTrueWhenReferenceAndVersionAreTheSame() {
    boolean jdkVersionSupported =
        JavaRuntimeVersionChecker.isJdkVersionGreaterOrEqualToRef("11", "11");
    assertTrue(jdkVersionSupported);
  }

  @Test
  void returnTrueWhenVersionIsGreater() {
    boolean jdkVersionSupported =
        JavaRuntimeVersionChecker.isJdkVersionGreaterOrEqualToRef("11", "17");
    assertTrue(jdkVersionSupported);
  }
}
