package com.solarwinds.joboe.config;

public class JavaRuntimeVersionChecker {
    public static final String minVersionSupported = "1.8.0_252";
    public static boolean isJdkVersionSupported() {
        return JavaVersionComparator.compare(minVersionSupported, System.getProperty("java.version")) <= 0;
    }
}
