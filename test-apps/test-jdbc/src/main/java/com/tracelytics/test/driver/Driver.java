package com.tracelytics.test.driver;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;

import com.tracelytics.test.action.DatabaseType;

/**
 * Test case for https://github.com/librato/joboe/pull/615/files
 * @author pluk
 *
 */
public class Driver implements java.sql.Driver {
    static {
        try {
            Class.forName(DatabaseType.DERBY_EMBEDDED.getDriverClass());
        
            DriverManager.registerDriver(new Driver());
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    
    @Override
    public Connection connect(String url, Properties props) throws SQLException {
        if (url.startsWith("jdbc:test:")) {
            return new TestConnection();
        } else {
            return null;
        }
            
    }
    
    public String host(Properties props) {
        return props.getProperty("HOST", "localhost");
    }
    
    public int port(Properties props) {
        return Integer.parseInt(props.getProperty("PORT", "3306"));
    }
    
    public String database(Properties props) {
        return props.getProperty("DBNAME");
    }

    @Override
    public boolean acceptsURL(String url) throws SQLException {
        return url != null;
    }

    @Override
    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
        // TODO Auto-generated method stub
        return new DriverPropertyInfo[0];
    }

    @Override
    public int getMajorVersion() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getMinorVersion() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public boolean jdbcCompliant() {
        // TODO Auto-generated method stub
        return true;
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        // TODO Auto-generated method stub
        return null;
    }

}
