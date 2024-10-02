package com.tracelytics.test.action;

public enum DatabaseType {
    MYSQL(false, true, "com.mysql.jdbc.Driver"), 
    POSTGRES(false, false, "org.postgresql.Driver"), 
    ORACLE(false, false, "oracle.jdbc.driver.OracleDriver"), 
    //DB2(false, false, "COM.ibm.db2.jdbc.app.DB2Driver"), 
    DERBY_EMBEDDED(true, false, "org.apache.derby.jdbc.EmbeddedDriver"),
    DERBY_CLIENT(false, false, "org.apache.derby.jdbc.ClientDriver"), 
    HSQLDB_EMBEDDED(true, true, "org.hsqldb.jdbcDriver"),
    HSQLDB_CLIENT(false, true, "org.hsqldb.jdbc.JDBCDriver"),
    MSSQL(false, false, "com.microsoft.jdbc.sqlserver.SQLServerDriver"),
    MARIADB(false, true, "org.mariadb.jdbc.Driver"),
    TEST_ISSUE_615(true, false, "com.tracelytics.test.driver.Driver"); //for https://github.com/librato/joboe/pull/615/files
    
    
    private boolean embedded;
    private boolean supportProcedure;
    private String driverClass;
    
    private DatabaseType(boolean embedded, boolean supportProcedure, String driverClass) {
        this.embedded = embedded;
        this.supportProcedure = supportProcedure;
        this.driverClass = driverClass;
    }
    
    public boolean isEmbedded() {
        return embedded;
    }
    
    public boolean isSupportProcedure() {
        return supportProcedure;
    }
    
    public String getDriverClass() {
        return driverClass;
    }
}