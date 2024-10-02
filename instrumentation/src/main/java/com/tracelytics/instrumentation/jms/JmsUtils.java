package com.tracelytics.instrumentation.jms;

/**
 * Some util methods that are not good to be put in anywhere else.
 */
public class JmsUtils {
    /**
     * Get the vendor name based on the class path.
     * @param className
     * @return
     */
    public static String getVendorName(String className) {
        if (className.contains("springframework")) {
            return "spring";
        } else if (className.contains("activemq")) {
            return "activemq";
        } else if (className.contains("weblogic")) {
            return "weblogic";
        } else {
            return null;
        }
    }
}
