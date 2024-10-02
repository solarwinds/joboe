package com.tracelytics.test.action;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.struts2.interceptor.SessionAware;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import com.opensymphony.xwork2.ActionSupport;
import com.opensymphony.xwork2.Preparable;
import com.zaxxer.hikari.HikariDataSource;

@SuppressWarnings("serial")
public abstract class AbstractJdbcAction extends ActionSupport implements Preparable, SessionAware {
    private List<String> extendedOutput;


    protected Map<String, Object> session;
    private DatabaseForm databaseForm;

    private String connectionString; //only for display purpose, not really used for actual database operation
    
    public static final String TEST_TABLE = "test_table";
    
    private static final Map<DatabaseType, String> DATABASE_CREATION_MAP = new HashMap<DatabaseType, String>();
    private static final Map<DatabaseType, String> TABLE_CREATION_MAP = new HashMap<DatabaseType, String>();
    
    static {
        DATABASE_CREATION_MAP.put(DatabaseType.MARIADB, "CREATE DATABASE IF NOT EXISTS %s");
        
        TABLE_CREATION_MAP.put(DatabaseType.MYSQL, "CREATE TABLE IF NOT EXISTS " + TEST_TABLE + "(id INTEGER UNSIGNED NOT NULL AUTO_INCREMENT, first_name VARCHAR(255) NOT NULL, last_name VARCHAR(255) NOT NULL, b BLOB(1000), PRIMARY KEY (id));");
        TABLE_CREATION_MAP.put(DatabaseType.POSTGRES, "CREATE TABLE IF NOT EXISTS " + TEST_TABLE + "(id SERIAL, first_name VARCHAR(255) NOT NULL, last_name VARCHAR(255) NOT NULL, b BLOB(1000), PRIMARY KEY (id));");
        TABLE_CREATION_MAP.put(DatabaseType.ORACLE, "CREATE TABLE IF NOT EXISTS " + TEST_TABLE + "(id INTEGER UNSIGNED NOT NULL AUTO_INCREMENT, first_name VARCHAR(255) NOT NULL, last_name VARCHAR(255) NOT NULL, b BLOB(1000), PRIMARY KEY (id));");
        TABLE_CREATION_MAP.put(DatabaseType.DERBY_CLIENT, "CREATE TABLE " + TEST_TABLE + "(id INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 0, INCREMENT BY 1), first_name VARCHAR(255) NOT NULL, last_name VARCHAR(255) NOT NULL, b BLOB(1000), CONSTRAINT primary_key PRIMARY KEY (id))");
        TABLE_CREATION_MAP.put(DatabaseType.DERBY_EMBEDDED, "CREATE TABLE " + TEST_TABLE + "(id INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 0, INCREMENT BY 1), first_name VARCHAR(255) NOT NULL, last_name VARCHAR(255) NOT NULL, b BLOB(1000), CONSTRAINT primary_key PRIMARY KEY (id))");
        TABLE_CREATION_MAP.put(DatabaseType.HSQLDB_CLIENT, "CREATE TABLE IF NOT EXISTS " + TEST_TABLE + "(id IDENTITY, first_name VARCHAR(255) NOT NULL, last_name VARCHAR(255) NOT NULL, b BLOB(1000));");
        TABLE_CREATION_MAP.put(DatabaseType.HSQLDB_EMBEDDED, "CREATE TABLE IF NOT EXISTS " + TEST_TABLE + "(id IDENTITY, first_name VARCHAR(255) NOT NULL, last_name VARCHAR(255) NOT NULL, b BLOB(1000));");
        TABLE_CREATION_MAP.put(DatabaseType.MSSQL, "CREATE TABLE IF NOT EXISTS " + TEST_TABLE + "(id INTEGER UNSIGNED NOT NULL AUTO_INCREMENT, first_name VARCHAR(255) NOT NULL, last_name VARCHAR(255) NOT NULL, b BLOB(1000), PRIMARY KEY (id));");
        TABLE_CREATION_MAP.put(DatabaseType.MARIADB, "CREATE TABLE IF NOT EXISTS " + TEST_TABLE + "(id INTEGER UNSIGNED NOT NULL AUTO_INCREMENT, first_name VARCHAR(255) NOT NULL, last_name VARCHAR(255) NOT NULL, b BLOB(1000), PRIMARY KEY (id));");
        TABLE_CREATION_MAP.put(DatabaseType.TEST_ISSUE_615, "CREATE TABLE " + TEST_TABLE + "(id INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 0, INCREMENT BY 1), first_name VARCHAR(255) NOT NULL, last_name VARCHAR(255) NOT NULL, b BLOB(1000), CONSTRAINT primary_key PRIMARY KEY (id))");
    }
    
    
    public DatabaseForm getDatabaseForm() {
        return databaseForm ;
    }
    
    protected boolean initializeDb() throws FormInputException {
        try {
            DatabaseType databaseType = DatabaseType.valueOf(databaseForm.getType());
            String databaseCreation = DATABASE_CREATION_MAP.get(databaseType);
            if (databaseCreation != null) {
                Connection connectionWithoutDatabase = null;
                try {
                    connectionWithoutDatabase = getConnection(false);
                    databaseCreation = String.format(databaseCreation, databaseForm.getDatabase());
                    Statement createDatabaseStatement = connectionWithoutDatabase.createStatement();
                    createDatabaseStatement.executeQuery(databaseCreation);
                } finally {
                    if (connectionWithoutDatabase != null) {
                        connectionWithoutDatabase.close();
                    }
                }
            }
            Connection connection = null;
            try {
                connection = getConnection(true);
                Statement createTableStatement = connection.createStatement();
                createTableStatement.execute(TABLE_CREATION_MAP.get(databaseType));
            } catch (SQLException e) {
                if (e.getMessage() != null && e.getMessage().contains("already exists")) { //then it's fine, some driver like Derby does not provide IF NOT EXIST syntax
                    
                } else {
                    throw e;
                }
            } finally {
                if (connection != null) {
                    connection.close();
                }
            }

            resetRecords();
            return true;
        } catch (Exception e) {
            printToOutput(e.getMessage(), e.getStackTrace());
            return false;
        } 
        
        
    }
    
    private Connection getDatabaseConnection() throws Exception {
        DatabaseType databaseType = DatabaseType.valueOf(databaseForm.getType());

        String host = databaseForm.getHost();
        Integer port;
        String portString = databaseForm.getPort().trim();
        if ("".equals(portString)) {
            port = null;
        } else {
            try {
                port = Integer.parseInt(portString);
            } catch (NumberFormatException e) {
                throw new FormInputException("databaseForm.port", "Invalid port value. Enter either valid integer or leave empty", e);
            }
        }
        
        String databaseUser = databaseForm.getUser();
        String databasePassword = databaseForm.getPassword();
        String databaseName = databaseForm.getDatabase();
        
        String baseString;
        
        switch (databaseType) {
        case MYSQL:
            baseString = "jdbc:mysql://" + host + (port != null ? ":" + port : "");
            connectionString = baseString  + "/";
            break;
        case POSTGRES:
            baseString = "jdbc:postgresql://" + host + (port != null ? ":" + port : "") + "/";
            connectionString = baseString;
            break;
        case ORACLE:
            baseString = "jdbc:oracle:thin:@//" + host + (port != null ? ":" + port : "");
            connectionString = baseString  + "/";
            break;
//            case DB2:
//                baseString = "jdbc:db2//" + host + ":" + port;
//                connectionString = baseString  + "/";
//                break;
        case DERBY_CLIENT:
            baseString = "jdbc:derby://" + host + (port != null ? ":" + port : "") + "/" + databaseName + ";create=true";
            connectionString = baseString;
            break;
        case DERBY_EMBEDDED:
            baseString = "jdbc:derby:" + databaseName + ";create=true";
            connectionString = baseString;
            break;
        case HSQLDB_CLIENT:
            baseString = "jdbc:hsqldb:hsql://" + host + (port != null ? ":" + port :  "") + "/" + databaseName;
            connectionString = baseString + "/" + databaseName;
            break;
        case HSQLDB_EMBEDDED:
            baseString = "jdbc:hsqldb:mem:" + databaseName;
            connectionString = baseString;
            break;
        case MSSQL:
            baseString = "jdbc:sqlserver://" + host + (port != null ? ":" + port : "");
            connectionString = baseString + ";databaseName=";
            break;
        case MARIADB:
            baseString = "jdbc:mariadb://" + host + (port != null ? ":" + port : "");
            connectionString = baseString  + "/";
            break;
        case TEST_ISSUE_615:
            baseString = "jdbc:test:" + databaseName + ";create=true";
            connectionString = baseString  + "/";
            break;
        default:
            System.err.println("Unexpected database type: " + databaseType);
            return null;
        }
            
        PoolingType poolingType = PoolingType.valueOf(databaseForm.getPooling());
        return databaseType.isEmbedded() ? getEmbeddedDatabaseConnection(baseString, databaseType, poolingType) : getDatabaseConnection(baseString, databaseUser, databasePassword, databaseType, poolingType);
        
    }
    
    private Connection getDatabaseConnection(String baseString, String databaseUser, String databasePassword, DatabaseType databaseType, PoolingType poolingType) throws Exception {
        if (poolingType == PoolingType.C3P0) {
            ComboPooledDataSource dataSource = new ComboPooledDataSource();
            dataSource.setDriverClass(databaseType.getDriverClass()); //loads the jdbc driver            
            dataSource.setJdbcUrl(baseString);
            dataSource.setUser(databaseUser);                                  
            dataSource.setPassword(databasePassword);
            return dataSource.getConnection();
        } else if (poolingType == PoolingType.HIKARI) {
            HikariDataSource dataSource = new HikariDataSource();
            dataSource.setDriverClassName(databaseType.getDriverClass()); //loads the jdbc driver            
            dataSource.setJdbcUrl(baseString);
            dataSource.setUsername(databaseUser);                                  
            dataSource.setPassword(databasePassword);
            return dataSource.getConnection();
        } else {
            Class.forName(databaseType.getDriverClass());
            return DriverManager.getConnection(baseString, databaseUser, databasePassword);
        }
    }

    private Connection getEmbeddedDatabaseConnection(String baseString, DatabaseType databaseType, PoolingType poolingType) throws Exception {
        if (poolingType == PoolingType.C3P0) {
            ComboPooledDataSource dataSource = new ComboPooledDataSource();
            dataSource.setDriverClass(databaseType.getDriverClass()); //loads the jdbc driver            
            dataSource.setJdbcUrl(baseString);
            return dataSource.getConnection(); 
        } else if (poolingType == PoolingType.HIKARI) {
            HikariDataSource dataSource = new HikariDataSource();
            dataSource.setDriverClassName(databaseType.getDriverClass()); //loads the jdbc driver            
            dataSource.setJdbcUrl(baseString);
            return dataSource.getConnection();
        } else {
            Class.forName(databaseType.getDriverClass());
            return DriverManager.getConnection(baseString);
        }
    }

    protected Connection getConnection() throws Exception {
        return getConnection(true);
    }
    
    protected Connection getConnection(boolean setDatabase) throws Exception {
        Connection connection = getDatabaseConnection();
        DatabaseType databaseType = DatabaseType.valueOf(databaseForm.getType());
        
        if (setDatabase && 
            databaseType != DatabaseType.DERBY_EMBEDDED && 
            databaseType != DatabaseType.DERBY_CLIENT && 
            databaseType != DatabaseType.HSQLDB_EMBEDDED &&
            databaseType != DatabaseType.HSQLDB_CLIENT) {
            connection.setCatalog(databaseForm.getDatabase());
        
        }
        
        return connection;
    }
    
    
    
    private void resetRecords() throws Exception {
        Connection connection = null;
        try {
            connection = getConnection();
            connection.createStatement().execute("TRUNCATE TABLE " + TEST_TABLE);
            
            insertRecord(connection, "Ben", "Murray");
            insertRecord(connection, "Alan", "Clark");
            insertRecord(connection, "Betty", "White");
            insertRecord(connection, "Jane", "Lee");
            insertRecord(connection, "Linus", "Lee");
            insertRecord(connection, "Charlie", "Brown");
            insertRecord(connection, "Sally", "Brown");
            insertRecord(connection, "Patricia", "Peppermint");
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    printToOutput(e.getMessage(), e.getStackTrace());
                }
            }
        }
    }
    
    private void insertRecord(Connection connection, String firstName, String lastName) throws SQLException {
        PreparedStatement statement = connection.prepareStatement("INSERT INTO " + TEST_TABLE + "(first_name, last_name) VALUES (?, ?)");
        statement.setString(1, firstName);
        statement.setString(2, lastName);
        statement.execute();
    }

    @Override
    public String execute() throws Exception {
        Connection connection = null;  
        try {
            connection = getConnection();
            execute(connection);
            
            return SUCCESS;
        } catch (Exception e) {
            e.printStackTrace();
            printToOutput(e.getMessage(), e.getStackTrace());
            return ERROR;
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    printToOutput(e.getMessage(), e.getStackTrace());
                }
            }
        }
    }
    
    protected abstract String execute(Connection connection) throws Exception;
    
   
    
    
    public List<String> getExtendedOutput() {
        return extendedOutput;
    }
    
    

    public void setExtendedOutput(List<String> extendedOutput) {
        this.extendedOutput = extendedOutput;
    }

    public void appendExtendedOutput(String text) {
        if (extendedOutput == null) {
            extendedOutput = new LinkedList<String>();
        }
        extendedOutput.add(text);
    }

    @Override
    public void prepare() throws Exception {
        extendedOutput = null; //clear the output
        if (!session.containsKey("databaseForm")) {
            session.put("databaseForm", new DatabaseForm());
        }
        
        databaseForm = (DatabaseForm) session.get("databaseForm");
    }

    protected void printToOutput(String title, Map<?, ?> map) {
        if (title != null) {
            appendExtendedOutput(title);
        }
        
        for (Object element : map.entrySet()) {
            appendExtendedOutput(element != null ? element.toString() : "null");
        }
    }
    
    protected void printToOutput(String title, List<?> keys) {
        if (title != null) {
            appendExtendedOutput(title);
        }
        
        for (Object element : keys) {
            appendExtendedOutput(element != null ? element.toString() : "null");
        }
    }
    
    protected void printToOutput(String title, Object...items) {
        printToOutput(title, Arrays.asList(items));
    }
    
    public String getConnectionString() {
        return connectionString;
    }
    
    @Override
    public void setSession(Map<String, Object> session) {
        this.session = session;
    }
    
    
}
