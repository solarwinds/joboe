package com.tracelytics.util;

public class JavaRuntimeVersionChecker {
    public static final String minVersionSupported = "1.8_252"; // TODO
    public static boolean isJdkVersionSupported() {
        return JavaVersionComparator.compare(minVersionSupported, System.getProperty("java.version")) <= 0;
    }
}
