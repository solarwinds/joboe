package com.tracelytics.instrumentation.jdbc;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

class JdbcDriverUtil {
    private static final Set<String> COMMON_PACKAGE_PREFIXES =  new HashSet<String>(Arrays.asList("com", "io", "net", "org"));
    
    private JdbcDriverUtil()
    {
    }
    
    static String getFlavorName(String packageName) {
        DriverVendor driverVendor = DriverVendor.fromPackageName(packageName);
        if (driverVendor != DriverVendor.UNKNOWN) {
            return driverVendor.label; //use the label as flavor name;
        } else {
            String flavorName = parseFlavorNameFromPackageName(packageName);
            return flavorName != null ? flavorName : "jdbc";
        }
    }
    
    /**
     * If the package name starts with a "common" prefix defined in COMMON_PACKAGE_PREFIXES,
     * then extracts the 2nd portion of the package name converted to lowercase('.' as delimiter) if available.
     * 
     * Otherwise return null.
     * 
     * @param packageName
     * @return
     */
    static String parseFlavorNameFromPackageName(String packageName) {
        if (packageName != null) {
            packageName = packageName.toLowerCase();
            int startMarker = packageName.indexOf('.');
            if (startMarker != -1) {
                String prefix = packageName.substring(0, startMarker);
                if (COMMON_PACKAGE_PREFIXES.contains(prefix)) {
                    packageName = packageName.substring(startMarker + 1);
                    int endMarker = packageName.indexOf('.');
                    return endMarker != -1 ? packageName.substring(0, endMarker) : packageName;
                }
            }
        }
        return null;
    }
}
