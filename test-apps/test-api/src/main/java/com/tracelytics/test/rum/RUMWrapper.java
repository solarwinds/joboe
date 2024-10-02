/**
 * Wraps RUM methods using reflection.
 * (works even if RUM classes are unavailable.)
 */
 
package com.tracelytics.test.rum;
import java.lang.reflect.Method;


public class RUMWrapper {

    private static Method headerMethod = null, footerMethod = null;
    
    static {
        try {
            /* Attempt to load RUM class and find header/footer methods: */
            ClassLoader classLoader = RUMWrapper.class.getClassLoader();
            Class rumClass = classLoader.loadClass("com.tracelytic.api.RUM");
            headerMethod = rumClass.getDeclaredMethod("getHeader");  
            footerMethod = rumClass.getDeclaredMethod("getFooter");  

        } catch (Exception e) {
            /* This is expected in cases where the Tracelytics jar is not available */
            System.err.println("RUM not available"); 
        }
    }

    public static String getHeader() {
        if (headerMethod == null) {
            return "";
        }

        try {
            return (String)headerMethod.invoke(null);
        } catch(Throwable ex) {
            // Should never happen
            return "";
        }
    }

    public static String getFooter() {
        if (footerMethod == null) {
            return "";
        }

        try {
            return (String)footerMethod.invoke(null);
        } catch(Throwable ex) {
            // Should never happen
            return "";
        }
    }
}

