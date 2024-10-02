/**
 * Runs queries against a remote DB2 database
 *
 * This can be setup with an AMI: https://aws.amazon.com/amis/ibm-db2-express-c-9-7-5-on-linux-64-bit-development-use-only (ami-0bde0b62) 
 * You'll also need the JDBC driver from http://www-304.ibm.com/support/docview.wss?rs=4020&uid=swg21385217
 * "IBM Data Server Driver for JDBC and SQLJ (JCC Driver)"
 *
 * The instance should be named 'db2inst1' (along with the default user), with a password of 'tracer' 
 *
 * Also make sure you set up the sample database with the db2sampl command:
 *   http://publib.boulder.ibm.com/infocenter/db2luw/v9r5/index.jsp?topic=%2Fcom.ibm.db2.luw.apdv.samptop.doc%2Fdoc%2Fr0001094.html
 *
 * Add an entry to your hosts file for 'devibmdb2' pointing to that host.
 */

package com.tracelytics.test;

import com.appoptics.api.ext.*;
import java.sql.*;
 
public class TestDB2 {

    private Connection conn = null;

    public void run() throws Exception {
        TraceEvent event = Trace.startTrace("TestDB2");
        event.addInfo("URL","/db2/test");
        event.report();

        dbConnect();
        insertRow();
        runQuery();
        callProc();

        conn.close();

        Trace.endTrace("TestDB2");
    }


    public void dbConnect() throws Exception {
        Class.forName ("com.ibm.db2.jcc.DB2Driver");
        conn = DriverManager.getConnection ("jdbc:db2://devibmdb2:50001/sample", "db2inst1", "tracer");
    }

    public void insertRow() throws Exception {
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("select count(deptno) from dept");
        int max = 0;
        if (rs.next()) { 
            max = rs.getInt(1);
        }
        rs.close();
        stmt.close();

        PreparedStatement ps = conn.prepareStatement("insert into dept(deptno, deptname, location, admrdept) values (?, ?, ?, ?)");
        ps.setInt(1, ++max);
        ps.setString(2, "TestName " + max);
        ps.setString(3, "TestLoc " + max);
        ps.setString(4, "A00");
        ps.executeUpdate();
        ps.close();
    }

    public void callProc() throws Exception {

        CallableStatement cs = conn.prepareCall("CALL bonus_increase(?, ?, ?, ?, ?, ?)");
        cs.setDouble(1, 1.0);
        cs.setDouble(2, 50000.00);
        cs.registerOutParameter(3, Types.VARCHAR);
        cs.registerOutParameter(4, Types.INTEGER);
        cs.registerOutParameter(5, Types.INTEGER);
        cs.registerOutParameter(6, Types.VARCHAR);
        ResultSet rs = cs.executeQuery();
        rs.close();
        cs.close();
    }

    public void runQuery() throws Exception {
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("select deptno, deptname from dept order by deptno");
        while (rs.next()) { 
            System.out.println(rs.getString(1) + "   " + rs.getString(2));
        }
        rs.close();
        stmt.close();
    }


    public static void main(String args[]) throws Exception {
        TestDB2 test = new TestDB2();
        test.run();
    }
}
