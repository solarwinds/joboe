/**
 * Runs queries against a remote Oracle DB
 *
 * This can be setup with an AMI: Oracle Database 11.2.0.1 64-bit Standard Edition One (ami-31739c58) 
 * You'll also need the JDBC driver from http://www.oracle.com/technetwork/database/features/jdbc/index-091264.html
 * Add an entry to your hosts file for 'devoracle' pointing to that host.
 *
 * This program assumes the default Oracle demo user scott/tiger is enabled.
 */

package com.tracelytics.test;

import java.sql.*;

import com.appoptics.api.ext.*;
/**
 * PRE: This is a test case for Oracle 11g 
 * 
 * Make sure the pom.xml of test-app has the correct Oracle JDBC driver enabled (11.x) 
 * 
 * Setup: 
 * 1. Download the Oracle Express Edition and install to the system
 * 2. Run script utlsampl.sql, the script usually is located within the Oracle installation directory
 *    a. Run SQL in command line (sqlplus locate in the bin folder)
 *    b. Connect with the default user (SYSTEM)
 *    c. Run the script utlsampl 
 *       i.For Windows SQL> {@literal @} c:\<script folder>\\utlsampl.sql
 *       ii.For Linux SQL> START /<script folder>/utlsampl.sql
 * 3. Verify user SCOTT is created via (might have to connect to sqlplus again) 
 *    SQL> SELECT username, account_status FROM dba_users;
 * 4. Add service name (listener) 'oracle'
 *    SQL> alter SYSTEM set service_names='oracle';
 *    SQL> alter SYSTEM register;
 * 5. Add host name devoracle and devoracle2 to the client which runs this test. Both of those host names should map to the Oracle DB server (could be the same as the client)
 * 6. Run this testcase with -javaagent on (sample_rate 100%). Traces should be reported on the front-end, make sure the host name reported are correct.   
 *    
 * 
 */
public class TestOracle {

    private Connection conn = null;

    public void run() throws Exception {
        TraceEvent event = Trace.startTrace("TestOracle");
        event.addInfo("URL","/oracle/test");
        event.report();

        //test with simple DB URL
        dbConnect();
        insertRow();
        runQuery();
        callProc();
        removeRow();

        conn.close();
        
        //test with DB URL in tnsnames.ora format
        dbConnectTnsnames();
        insertRow();
        runQuery();
        removeRow();
        callProc();

        conn.close();

        Trace.endTrace("TestOracle");
    }


    private void removeRow() throws SQLException {
        PreparedStatement ps = conn.prepareStatement("DELETE FROM dept WHERE DNAME LIKE '%TestName%'");
        ps.executeUpdate();
        ps.close();
    }


    public void dbConnect() throws Exception {
        Class.forName ("oracle.jdbc.OracleDriver");
        conn = DriverManager.getConnection ("jdbc:oracle:thin:@//devoracle:1521/oracle", "scott", "tiger");
    }
    
    public void dbConnectTnsnames() throws Exception {
        Class.forName ("oracle.jdbc.OracleDriver");
        conn = DriverManager.getConnection("jdbc:oracle:thin:@(DESCRIPTION=(LOAD_BALANCE=on)(ADDRESS=(PROTOCOL=TCP)(HOST=devoracle)(PORT=1521))(CONNECT_DATA=(SID=xe))(ADDRESS=(PROTOCOL=TCP)(HOST=devoracle2)(PORT=1521))(CONNECT_DATA=(SID=xe)))", "scott", "tiger");
    }

    public void insertRow() throws Exception {
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("select max(deptno) from dept");
        int max = 0;
        if (rs.next()) { 
            max = rs.getInt(1);
        }
        rs.close();
        stmt.close();

        PreparedStatement ps = conn.prepareStatement("insert into dept(deptno, dname, loc) values (?, ?, ?)");
        ps.setInt(1, ++max);
        ps.setString(2, "TestName " + max);
        ps.setString(3, "TestLoc " + max);
        ps.executeUpdate();
        ps.close();
    }

    public void callProc() throws Exception {
        CallableStatement cs = conn.prepareCall("begin dbms_output.put_line(?); end;");
        cs.setString(1, "test");
        cs.executeUpdate();
    }

    public void runQuery() throws Exception {
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("select dname, loc from dept order by deptno");
        while (rs.next()) { 
            System.out.println(rs.getString(1) + "   " + rs.getString(2));
        }
        rs.close();
        stmt.close();
    }


    public static void main(String args[]) throws Exception {
        TestOracle test = new TestOracle();
        test.run();
    }
}
