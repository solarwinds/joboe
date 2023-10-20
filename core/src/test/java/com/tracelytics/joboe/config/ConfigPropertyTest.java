package com.tracelytics.joboe.config;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ConfigPropertyTest extends TestCase {

    public void testThatKeysAreNotDuplicated() {
        List<String> fileKey = new ArrayList<>();
        List<String> envKey = new ArrayList<>();

        Arrays.stream(ConfigProperty.values()).forEach(configProperty -> {
                    if (configProperty.getConfigFileKey() != null)
                        fileKey.add(configProperty.getConfigFileKey());

                    if (configProperty.getEnvironmentVariableKey() != null)
                        envKey.add(configProperty.getEnvironmentVariableKey());
                }
        );

        long fileKeyCount = fileKey.stream().distinct().count();
        long envKeyCount = envKey.stream().distinct().count();

        assertEquals(fileKey.size(), fileKeyCount);
        assertEquals(envKey.size(), envKeyCount);
    }
}