package com.solarwinds.joboe.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

public class ConfigPropertyTest {

  @Test
  public void testThatKeysAreNotDuplicated() {
    List<String> fileKey = new ArrayList<>();
    List<String> envKey = new ArrayList<>();

    Arrays.stream(ConfigProperty.values())
        .forEach(
            configProperty -> {
              if (configProperty.getConfigFileKey() != null)
                fileKey.add(configProperty.getConfigFileKey());

              if (configProperty.getEnvironmentVariableKey() != null)
                envKey.add(configProperty.getEnvironmentVariableKey());
            });

    long fileKeyCount = fileKey.stream().distinct().count();
    long envKeyCount = envKey.stream().distinct().count();

    assertEquals(fileKey.size(), fileKeyCount);
    assertEquals(envKey.size(), envKeyCount);
  }
}
