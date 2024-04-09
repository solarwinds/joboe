package com.solarwinds.joboe.config;

public class JavaRuntimeVersionChecker {
    public static final String minVersionSupported = "1.8.0_252";
    public static boolean isJdkVersionSupported() {
        return isJdkVersionGreaterOrEqualToRef(minVersionSupported, System.getProperty("java.version"));
    }

    public static boolean isJdkVersionGreaterOrEqualToRef(String reference, String version) {
        return JavaVersionComparator.compare(reference, version) <= 0;
    }
}
