package com.tracelytics.instrumentation.jdbc;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

enum DriverVendor {
    MYSQL("mysql", "mysql"),
    POSTGRES("postgresql", "postgresql"),
    DERBY("derby", "derby"),
    HSQLDB("hsqldb", "hsqldb"),
    ORACLE("oracle", "oracle"),
    DB2("db2", "db2"),
    SYBASE("sybase", "sybase"),
    MSSQL("mssql", "microsoft"),
    MARIADB("mariadb", "mariadb"),
    IBM_AS400("ibm-as400-db", "com.ibm.as400"),
    DBCP("dbcp", "dbcp", true),
    C3P0("c3p0", "c3p0", true),
    JBOSS("jboss", "jboss", true),
    HIKARI("hikari", "hikari", true),
    SUN_PROXY("sun-db-proxy", "com.sun.proxy", true),
    DATANUCLUES_RDBMS("datanucleus-rdbms", "org.datanucleus.store.rdbms", true),
    UNKNOWN("unknown", null);
    
    String label;
    boolean isWrapper;
    private String packageSegment;
    private static final Map<String, DriverVendor> packageSegmentToVendor = new HashMap<String, DriverVendor>();
    static {
        for (DriverVendor vendor : DriverVendor.values()) {
            if (vendor.packageSegment != null) {
                packageSegmentToVendor.put(vendor.packageSegment, vendor);
            }
        }
    }

    private DriverVendor(String label, String packageSegment) {
        this(label, packageSegment, false);
    }
    
    private DriverVendor(String label, String packageSegment, boolean isWrapper) {
        this.label = label;
        this.isWrapper = isWrapper;
        this.packageSegment = packageSegment;
    }
    
    /**
     * Finds the corresponding DriverVendor with package segment matches part of the packageName
     * 
     * If no match, DriverVendor.UNKNOWN is returned
     * 
     * @param packageSegment
     * @return
     */
    public static DriverVendor fromPackageName(String packageName) {
        if (packageName != null) {
            packageName = packageName.toLowerCase();
            for (Entry<String, DriverVendor> entry : packageSegmentToVendor.entrySet()) {
                String segment = entry.getKey();
                if (packageName.contains(segment)) {
                    return entry.getValue();
                }
            }
        }
        
        return DriverVendor.UNKNOWN;
    }
}