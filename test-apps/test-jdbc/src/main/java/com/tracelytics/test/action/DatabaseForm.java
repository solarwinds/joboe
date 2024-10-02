package com.tracelytics.test.action;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DatabaseForm {
    private static final String DEFAULT_HOST = "localhost";
    private static final String DEFAULT_PORT = "";
    private static final String DEFAULT_USER = "";
    private static final String DEFAULT_PASSWORD = "";
    private static final String DEFAULT_DATABASE = "test_database";
    
    private static final List<String> DATABASE_TYPES;
    
    static {
        List<String> databaseTypes = new ArrayList<String>();
        for (DatabaseType type : DatabaseType.values()) {
            databaseTypes.add(type.name());
        }
        DATABASE_TYPES = Collections.unmodifiableList(databaseTypes);
    }
    
    private static final List<String> POOLING_TYPES;
    
    static {
        List<String> poolingTypes = new ArrayList<String>();
        for (PoolingType type : PoolingType.values()) {
            poolingTypes.add(type.name());
        }
        POOLING_TYPES = Collections.unmodifiableList(poolingTypes);
    }
    
    
    
    private String host = DEFAULT_HOST;
    private String port = DEFAULT_PORT;
    private String database = DEFAULT_DATABASE;
    private String user = DEFAULT_USER;
    private String password = DEFAULT_PASSWORD;
    private String type;
    private String pooling;
    
    public String getHost() {
        return host;
    }
    public void setHost(String host) {
        this.host = host;
    }
    public String getPort() {
        return port;
    }
    public void setPort(String port) {
        this.port = port;
    }
    public String getDatabase() {
        return database;
    }
    public void setDatabase(String database) {
        this.database = database;
    }
    public String getUser() {
        return user;
    }
    public void setUser(String user) {
        this.user = user;
    }
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }
    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }
    public String getPooling() {
        return pooling;
    }
    public void setPooling(String pooling) {
        this.pooling = pooling;
    }
    
    
    public List<String> getTypes() {
       return DATABASE_TYPES;
    }
    
    public List<String> getPoolings() {
        return POOLING_TYPES;
    }
}
