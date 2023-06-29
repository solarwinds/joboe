package com.tracelytics.joboe.config;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ConfigPropertyTest extends TestCase {

    public void testThatKeysAreNotDuplicated() {
        List<String> fileKey = new ArrayList<>();
        List<String> envKey = new ArrayList<>();
        List<String> argKey = new ArrayList<>();

        Arrays.stream(ConfigProperty.values()).forEach(configProperty -> {
                    if (configProperty.getConfigFileKey() != null)
                        fileKey.add(configProperty.getConfigFileKey());

                    if (configProperty.getEnvironmentVariableKey() != null)
                        envKey.add(configProperty.getEnvironmentVariableKey());

                    String reduce = Arrays.stream(configProperty.getAgentArgumentKeys()).reduce("", (a, b) -> a + b);
                    if (!reduce.isEmpty())
                        argKey.add(reduce);
                }
        );

        long fileKeyCount = fileKey.stream().distinct().count();
        long envKeyCount = envKey.stream().distinct().count();
        long argKeyCount = argKey.stream().distinct().count();

        assertEquals(fileKey.size(), fileKeyCount);
        assertEquals(envKey.size(), envKeyCount);
        assertEquals(argKey.size(), argKeyCount);
    }
}