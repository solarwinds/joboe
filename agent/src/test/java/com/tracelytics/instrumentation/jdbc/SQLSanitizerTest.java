package com.tracelytics.instrumentation.jdbc;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import junit.framework.TestCase;

public class SQLSanitizerTest extends TestCase {
    private static final List<SQLCase> TEST_CASES = new ArrayList<SQLCase>();
    
    static {
        initializeTestCases();
    }
    
    public void testSanitize() {
      for (SQLCase testCase : TEST_CASES) {
          String sourceQuery = testCase.sourceQuery;
          for (Entry<SQLCaseConf, String> expectedEntry : testCase.expectedResults.entrySet()) {
              String expectedQuery = expectedEntry.getValue();
              SQLCaseConf conf = expectedEntry.getKey();
              assertEquals("Source sql: [" + sourceQuery + "]", expectedQuery , SQLSanitizer.getSanitizer(conf.sanitizeFlag, conf.className).sanitizeQuery(sourceQuery));
          }
      }
      
    }
    
    private static void initializeTestCases() {
        SQLCase sqlCase;
        
     // Empty strings
        sqlCase = new SQLCase("select * from tbl where name = '';");
        sqlCase.setExpectedResult(new SQLCaseConf(SQLSanitizer.ENABLED_AUTO), "select * from tbl where name = ?;");
        sqlCase.setExpectedResult(new SQLCaseConf(SQLSanitizer.ENABLED_DROP_DOUBLE_QUOTED), "select * from tbl where name = ?;");
        sqlCase.setExpectedResult(new SQLCaseConf(SQLSanitizer.ENABLED_KEEP_DOUBLE_QUOTED), "select * from tbl where name = ?;");
        TEST_CASES.add(sqlCase);
        
        // PostgreSQL Bit-String
        sqlCase = new SQLCase("select * from tbl where name = B'0101';");
        sqlCase.setExpectedResult(new SQLCaseConf(SQLSanitizer.ENABLED_AUTO), "select * from tbl where name = ?;");
        sqlCase.setExpectedResult(new SQLCaseConf(SQLSanitizer.ENABLED_DROP_DOUBLE_QUOTED), "select * from tbl where name = ?;");
        sqlCase.setExpectedResult(new SQLCaseConf(SQLSanitizer.ENABLED_KEEP_DOUBLE_QUOTED), "select * from tbl where name = ?;");
        TEST_CASES.add(sqlCase);
    
        // PostgreSQL escaped Unicode string
        sqlCase = new SQLCase("select * from tbl where name = U&'d\\0061t+000061';");
        sqlCase.setExpectedResult(new SQLCaseConf(SQLSanitizer.ENABLED_AUTO), "select * from tbl where name = ?;");
        sqlCase.setExpectedResult(new SQLCaseConf(SQLSanitizer.ENABLED_DROP_DOUBLE_QUOTED), "select * from tbl where name = ?;");
        sqlCase.setExpectedResult(new SQLCaseConf(SQLSanitizer.ENABLED_KEEP_DOUBLE_QUOTED), "select * from tbl where name = ?;");
        TEST_CASES.add(sqlCase);
    
        // PostgreSQL Hex-String
        sqlCase = new SQLCase("select * from tbl where name = X'FEFF';");
        sqlCase.setExpectedResult(new SQLCaseConf(SQLSanitizer.ENABLED_AUTO), "select * from tbl where name = ?;");
        sqlCase.setExpectedResult(new SQLCaseConf(SQLSanitizer.ENABLED_DROP_DOUBLE_QUOTED), "select * from tbl where name = ?;");
        sqlCase.setExpectedResult(new SQLCaseConf(SQLSanitizer.ENABLED_KEEP_DOUBLE_QUOTED), "select * from tbl where name = ?;");
        TEST_CASES.add(sqlCase);
    
        // PostgreSQL dollar-quoted string
        sqlCase = new SQLCase("select * from tbl where name = $$Dianne's horse$$;");
        sqlCase.setExpectedResult(new SQLCaseConf(SQLSanitizer.ENABLED_AUTO, "postgresql"), "select * from tbl where name = ?;");
        sqlCase.setExpectedResult(new SQLCaseConf(SQLSanitizer.ENABLED_DROP_DOUBLE_QUOTED, "postgresql"), "select * from tbl where name = ?;");
        sqlCase.setExpectedResult(new SQLCaseConf(SQLSanitizer.ENABLED_KEEP_DOUBLE_QUOTED, "postgresql"), "select * from tbl where name = ?;");
        TEST_CASES.add(sqlCase);
    
        // PostgreSQL tagged dollar-quoted string
        sqlCase = new SQLCase("select * from tbl where name = $myTag$Dianne's horse$myTag$;");
        sqlCase.setExpectedResult(new SQLCaseConf(SQLSanitizer.ENABLED_AUTO, "postgresql"), "select * from tbl where name = ?;");
        sqlCase.setExpectedResult(new SQLCaseConf(SQLSanitizer.ENABLED_DROP_DOUBLE_QUOTED, "postgresql"), "select * from tbl where name = ?;");
        sqlCase.setExpectedResult(new SQLCaseConf(SQLSanitizer.ENABLED_KEEP_DOUBLE_QUOTED, "postgresql"), "select * from tbl where name = ?;");
        TEST_CASES.add(sqlCase);
    
        // PostgreSQL dollar-quoted string with dollar signs in the literal
        sqlCase = new SQLCase("select * from tbl where name = $$Dianne's horse is $999$$;");
        sqlCase.setExpectedResult(new SQLCaseConf(SQLSanitizer.ENABLED_AUTO, "postgresql"), "select * from tbl where name = ?;");
        sqlCase.setExpectedResult(new SQLCaseConf(SQLSanitizer.ENABLED_DROP_DOUBLE_QUOTED, "postgresql"), "select * from tbl where name = ?;");
        sqlCase.setExpectedResult(new SQLCaseConf(SQLSanitizer.ENABLED_KEEP_DOUBLE_QUOTED, "postgresql"), "select * from tbl where name = ?;");
        TEST_CASES.add(sqlCase);
    
        // PostgreSQL tagged dollar-quoted string with dollar signs in the literal
        sqlCase = new SQLCase("select * from tbl where name = $tag$Dianne's horse is $999$tag$;");
        sqlCase.setExpectedResult(new SQLCaseConf(SQLSanitizer.ENABLED_AUTO, "postgresql"), "select * from tbl where name = ?;");
        sqlCase.setExpectedResult(new SQLCaseConf(SQLSanitizer.ENABLED_DROP_DOUBLE_QUOTED, "postgresql"), "select * from tbl where name = ?;");
        sqlCase.setExpectedResult(new SQLCaseConf(SQLSanitizer.ENABLED_KEEP_DOUBLE_QUOTED, "postgresql"), "select * from tbl where name = ?;");
        TEST_CASES.add(sqlCase);
    
        // PostgreSQL tagged dollar-quoted string with dollar signs in the literal
        sqlCase = new SQLCase("select * from tbl where name = $tag\\$Dianne's horse is $999\\$tag\\$ and gender = $tag$male$tag$;");
        sqlCase.setExpectedResult(new SQLCaseConf(SQLSanitizer.ENABLED_AUTO, "postgresql"), "select * from tbl where name = ? and gender = ?;");
        sqlCase.setExpectedResult(new SQLCaseConf(SQLSanitizer.ENABLED_DROP_DOUBLE_QUOTED, "postgresql"), "select * from tbl where name = ? and gender = ?;");
        sqlCase.setExpectedResult(new SQLCaseConf(SQLSanitizer.ENABLED_KEEP_DOUBLE_QUOTED, "postgresql"), "select * from tbl where name = ? and gender = ?;");
        TEST_CASES.add(sqlCase);
    
        // PostgreSQL tagged dollar-quoted string with dollar signs in the literal and Unicode emoji "👿👌 😋"
        sqlCase = new SQLCase("select * from tbl where name = $tag$Dianne's horse is \uD83D\uDC7F\uD83D\uDC4C \uD83D\uDE0B and costs $999$tag$ and gender = $tag$male$tag$;");
        sqlCase.setExpectedResult(new SQLCaseConf(SQLSanitizer.ENABLED_AUTO, "postgresql"), "select * from tbl where name = ? and gender = ?;");
        sqlCase.setExpectedResult(new SQLCaseConf(SQLSanitizer.ENABLED_DROP_DOUBLE_QUOTED, "postgresql"), "select * from tbl where name = ? and gender = ?;");
        sqlCase.setExpectedResult(new SQLCaseConf(SQLSanitizer.ENABLED_KEEP_DOUBLE_QUOTED, "postgresql"), "select * from tbl where name = ? and gender = ?;");
        TEST_CASES.add(sqlCase);
        
        // Empty strings
        sqlCase = new SQLCase("select * from tbl where name = '\uD83D\uDC7F';");
        sqlCase.setExpectedResult(new SQLCaseConf(SQLSanitizer.ENABLED_AUTO), "select * from tbl where name = ?;");
        sqlCase.setExpectedResult(new SQLCaseConf(SQLSanitizer.ENABLED_DROP_DOUBLE_QUOTED), "select * from tbl where name = ?;");
        sqlCase.setExpectedResult(new SQLCaseConf(SQLSanitizer.ENABLED_KEEP_DOUBLE_QUOTED), "select * from tbl where name = ?;");
        TEST_CASES.add(sqlCase);
    
        // Single-quoted strings
        sqlCase = new SQLCase("select * from tbl where name = 'private';");
        sqlCase.setExpectedResult(new SQLCaseConf(SQLSanitizer.ENABLED_AUTO), "select * from tbl where name = ?;");
        sqlCase.setExpectedResult(new SQLCaseConf(SQLSanitizer.ENABLED_DROP_DOUBLE_QUOTED), "select * from tbl where name = ?;");
        sqlCase.setExpectedResult(new SQLCaseConf(SQLSanitizer.ENABLED_KEEP_DOUBLE_QUOTED), "select * from tbl where name = ?;");
        TEST_CASES.add(sqlCase);
        
     // Double-quoted strings
        sqlCase = new SQLCase("select ssn from accounts where password = \"mypass\";");
        sqlCase.setExpectedResult(new SQLCaseConf(SQLSanitizer.ENABLED_AUTO), "select ssn from accounts where password = ?;");
        sqlCase.setExpectedResult(new SQLCaseConf(SQLSanitizer.ENABLED_DROP_DOUBLE_QUOTED), "select ssn from accounts where password = ?;");
        sqlCase.setExpectedResult(new SQLCaseConf(SQLSanitizer.ENABLED_KEEP_DOUBLE_QUOTED), "select ssn from accounts where password = \"mypass\";");
        TEST_CASES.add(sqlCase);
        
     // Double-quoted strings with embedded single quotes
        sqlCase = new SQLCase("select ssn from accounts where password = \"\\'\";");
        sqlCase.setExpectedResult(new SQLCaseConf(SQLSanitizer.ENABLED_AUTO), "select ssn from accounts where password = ?;");
        sqlCase.setExpectedResult(new SQLCaseConf(SQLSanitizer.ENABLED_DROP_DOUBLE_QUOTED), "select ssn from accounts where password = ?;");
        sqlCase.setExpectedResult(new SQLCaseConf(SQLSanitizer.ENABLED_KEEP_DOUBLE_QUOTED), "select ssn from accounts where password = \"\\'\";");
        TEST_CASES.add(sqlCase);
       
       // Double-quoted strings with embedded double quotes
        sqlCase = new SQLCase("select ssn from accounts where password = \"\\\"abc\\\"\";");
        sqlCase.setExpectedResult(new SQLCaseConf(SQLSanitizer.ENABLED_AUTO), "select ssn from accounts where password = ?;");
        sqlCase.setExpectedResult(new SQLCaseConf(SQLSanitizer.ENABLED_DROP_DOUBLE_QUOTED), "select ssn from accounts where password = ?;");
        sqlCase.setExpectedResult(new SQLCaseConf(SQLSanitizer.ENABLED_KEEP_DOUBLE_QUOTED), "select ssn from accounts where password = \"\\\"abc\\\"\";");
        TEST_CASES.add(sqlCase);
        
      // Single-back-quoted identifiers (ie. MySQL style)
        sqlCase = new SQLCase("select `ssn` from `accounts` where password = 'mypass';");
        sqlCase.setExpectedResult(new SQLCaseConf(SQLSanitizer.ENABLED_AUTO), "select `ssn` from `accounts` where password = ?;");
        sqlCase.setExpectedResult(new SQLCaseConf(SQLSanitizer.ENABLED_DROP_DOUBLE_QUOTED), "select `ssn` from `accounts` where password = ?;");
        sqlCase.setExpectedResult(new SQLCaseConf(SQLSanitizer.ENABLED_KEEP_DOUBLE_QUOTED), "select `ssn` from `accounts` where password = ?;");
        TEST_CASES.add(sqlCase);
        
     // Unicode
        sqlCase = new SQLCase("select col from tbl where name = '\\xE2\\x98\\x83 unicode'");
        sqlCase.setExpectedResult(new SQLCaseConf(SQLSanitizer.ENABLED_AUTO), "select col from tbl where name = ?");
        sqlCase.setExpectedResult(new SQLCaseConf(SQLSanitizer.ENABLED_DROP_DOUBLE_QUOTED), "select col from tbl where name = ?");
        sqlCase.setExpectedResult(new SQLCaseConf(SQLSanitizer.ENABLED_KEEP_DOUBLE_QUOTED), "select col from tbl where name = ?");
        TEST_CASES.add(sqlCase);
        
     // Time and date
        sqlCase = new SQLCase("select col from tbl where birth = '2013-01-26 11:03:30'");
        sqlCase.setExpectedResult(new SQLCaseConf(SQLSanitizer.ENABLED_AUTO), "select col from tbl where birth = ?");
        sqlCase.setExpectedResult(new SQLCaseConf(SQLSanitizer.ENABLED_DROP_DOUBLE_QUOTED), "select col from tbl where birth = ?");
        sqlCase.setExpectedResult(new SQLCaseConf(SQLSanitizer.ENABLED_KEEP_DOUBLE_QUOTED), "select col from tbl where birth = ?");
        TEST_CASES.add(sqlCase);
        
     //Decimals and Mantissa exponent
        sqlCase = new SQLCase("SELECT col FROM tbl WHERE dec < 0.678 && man > 1.23E-56");
        sqlCase.setExpectedResult(new SQLCaseConf(SQLSanitizer.ENABLED_AUTO), "SELECT col FROM tbl WHERE dec < ? && man > ?");
        sqlCase.setExpectedResult(new SQLCaseConf(SQLSanitizer.ENABLED_DROP_DOUBLE_QUOTED), "SELECT col FROM tbl WHERE dec < ? && man > ?");
        sqlCase.setExpectedResult(new SQLCaseConf(SQLSanitizer.ENABLED_KEEP_DOUBLE_QUOTED), "SELECT col FROM tbl WHERE dec < ? && man > ?");
        TEST_CASES.add(sqlCase);
        
        //Signed numbers, take note that ideally we should remove signs but it is very tricky to do it in state machine for example to differentiate:
        //identifier +number 
        //number +number
        sqlCase = new SQLCase("SELECT col FROM tbl WHERE dec < +0.678 && man > -1.23E-56");
        sqlCase.setExpectedResult(new SQLCaseConf(SQLSanitizer.ENABLED_AUTO), "SELECT col FROM tbl WHERE dec < +? && man > -?");
        sqlCase.setExpectedResult(new SQLCaseConf(SQLSanitizer.ENABLED_DROP_DOUBLE_QUOTED), "SELECT col FROM tbl WHERE dec < +? && man > -?");
        sqlCase.setExpectedResult(new SQLCaseConf(SQLSanitizer.ENABLED_KEEP_DOUBLE_QUOTED), "SELECT col FROM tbl WHERE dec < +? && man > -?");
        TEST_CASES.add(sqlCase);
        
        //Arithmetic operations. Take note that for decimal with just .n (no zero before decimal), it will be displayed as .? . It is quite a minor problem and
        //not worth adding a new state (we basically need a new state that is similar to COPY but is after reading the whitespace character, it will then goto NUMBER
        //state if it is .)
        sqlCase = new SQLCase("SELECT * FROM companies WHERE ((revenue - expense) * 0.1) > revenue AND revenue > 1000000;");
        sqlCase.setExpectedResult(new SQLCaseConf(SQLSanitizer.ENABLED_AUTO), "SELECT * FROM companies WHERE ((revenue - expense) * ?) > revenue AND revenue > ?;");
        sqlCase.setExpectedResult(new SQLCaseConf(SQLSanitizer.ENABLED_DROP_DOUBLE_QUOTED), "SELECT * FROM companies WHERE ((revenue - expense) * ?) > revenue AND revenue > ?;");
        sqlCase.setExpectedResult(new SQLCaseConf(SQLSanitizer.ENABLED_KEEP_DOUBLE_QUOTED), "SELECT * FROM companies WHERE ((revenue - expense) * ?) > revenue AND revenue > ?;");
        TEST_CASES.add(sqlCase);

     // Type declaration parameters
        sqlCase = new SQLCase("CREATE TABLE tbl (col1 VARCHAR(1000, 50), col2 CHAR(123, col3 DEC(16,2), col4 MONEY(10, 2))");
        sqlCase.setExpectedResult(new SQLCaseConf(SQLSanitizer.ENABLED_AUTO), "CREATE TABLE tbl (col1 VARCHAR(?, ?), col2 CHAR(?, col3 DEC(?,?), col4 MONEY(?, ?))");
        sqlCase.setExpectedResult(new SQLCaseConf(SQLSanitizer.ENABLED_DROP_DOUBLE_QUOTED), "CREATE TABLE tbl (col1 VARCHAR(?, ?), col2 CHAR(?, col3 DEC(?,?), col4 MONEY(?, ?))");
        sqlCase.setExpectedResult(new SQLCaseConf(SQLSanitizer.ENABLED_KEEP_DOUBLE_QUOTED), "CREATE TABLE tbl (col1 VARCHAR(?, ?), col2 CHAR(?, col3 DEC(?,?), col4 MONEY(?, ?))");
        TEST_CASES.add(sqlCase);
        
       // No spaces around operator
        sqlCase = new SQLCase("UPDATE extent_descriptor SET sample_rate=0.02, sample_source=NULL WHERE layer='worker' AND host='etl1.tlys.us' AND app_id=39");
        sqlCase.setExpectedResult(new SQLCaseConf(SQLSanitizer.ENABLED_AUTO), "UPDATE extent_descriptor SET sample_rate=?, sample_source=NULL WHERE layer=? AND host=? AND app_id=?");
        sqlCase.setExpectedResult(new SQLCaseConf(SQLSanitizer.ENABLED_DROP_DOUBLE_QUOTED), "UPDATE extent_descriptor SET sample_rate=?, sample_source=NULL WHERE layer=? AND host=? AND app_id=?");
        sqlCase.setExpectedResult(new SQLCaseConf(SQLSanitizer.ENABLED_KEEP_DOUBLE_QUOTED), "UPDATE extent_descriptor SET sample_rate=?, sample_source=NULL WHERE layer=? AND host=? AND app_id=?");
        TEST_CASES.add(sqlCase);
        
        // No spaces around parentheses        
        sqlCase = new SQLCase("INSERT INTO d_trace_stats (trace_id, trace_size, event_max_size, benchmark_queue, benchmark_since_added)VALUES(130580, 130536, 1314, NULL, NULL)");
        sqlCase.setExpectedResult(new SQLCaseConf(SQLSanitizer.ENABLED_AUTO), "INSERT INTO d_trace_stats (trace_id, trace_size, event_max_size, benchmark_queue, benchmark_since_added)VALUES(?, ?, ?, NULL, NULL)");
        sqlCase.setExpectedResult(new SQLCaseConf(SQLSanitizer.ENABLED_DROP_DOUBLE_QUOTED), "INSERT INTO d_trace_stats (trace_id, trace_size, event_max_size, benchmark_queue, benchmark_since_added)VALUES(?, ?, ?, NULL, NULL)");
        sqlCase.setExpectedResult(new SQLCaseConf(SQLSanitizer.ENABLED_KEEP_DOUBLE_QUOTED), "INSERT INTO d_trace_stats (trace_id, trace_size, event_max_size, benchmark_queue, benchmark_since_added)VALUES(?, ?, ?, NULL, NULL)");
        TEST_CASES.add(sqlCase);
        
        sqlCase = new SQLCase("select 1");
        sqlCase.setExpectedResult(new SQLCaseConf(SQLSanitizer.ENABLED_AUTO), "select ?");
        sqlCase.setExpectedResult(new SQLCaseConf(SQLSanitizer.ENABLED_DROP_DOUBLE_QUOTED), "select ?");
        sqlCase.setExpectedResult(new SQLCaseConf(SQLSanitizer.ENABLED_KEEP_DOUBLE_QUOTED), "select ?");
        TEST_CASES.add(sqlCase);
        
        sqlCase = new SQLCase("select `a` from `b`.`c` where `d` = `e` limit 10,100");
        sqlCase.setExpectedResult(new SQLCaseConf(SQLSanitizer.ENABLED_AUTO), "select `a` from `b`.`c` where `d` = `e` limit ?,?");
        sqlCase.setExpectedResult(new SQLCaseConf(SQLSanitizer.ENABLED_DROP_DOUBLE_QUOTED), "select `a` from `b`.`c` where `d` = `e` limit ?,?");
        sqlCase.setExpectedResult(new SQLCaseConf(SQLSanitizer.ENABLED_KEEP_DOUBLE_QUOTED), "select `a` from `b`.`c` where `d` = `e` limit ?,?");
        TEST_CASES.add(sqlCase);
        
        sqlCase = new SQLCase("select \"a\" from \"b.c\" where \"d\" = \"e\" limit 10,100");
        sqlCase.setExpectedResult(new SQLCaseConf(SQLSanitizer.ENABLED_AUTO), "select ? from ? where ? = ? limit ?,?");
        sqlCase.setExpectedResult(new SQLCaseConf(SQLSanitizer.ENABLED_DROP_DOUBLE_QUOTED), "select ? from ? where ? = ? limit ?,?");
        sqlCase.setExpectedResult(new SQLCaseConf(SQLSanitizer.ENABLED_KEEP_DOUBLE_QUOTED), "select \"a\" from \"b.c\" where \"d\" = \"e\" limit ?,?");
        TEST_CASES.add(sqlCase);
        
        sqlCase = new SQLCase("select `a` from `b`.`c` where `d` = 'e' and `f` = \"g\" limit 10,100");
        sqlCase.setExpectedResult(new SQLCaseConf(SQLSanitizer.ENABLED_AUTO), "select `a` from `b`.`c` where `d` = ? and `f` = ? limit ?,?");
        sqlCase.setExpectedResult(new SQLCaseConf(SQLSanitizer.ENABLED_DROP_DOUBLE_QUOTED), "select `a` from `b`.`c` where `d` = ? and `f` = ? limit ?,?");
        sqlCase.setExpectedResult(new SQLCaseConf(SQLSanitizer.ENABLED_KEEP_DOUBLE_QUOTED), "select `a` from `b`.`c` where `d` = ? and `f` = \"g\" limit ?,?");
        TEST_CASES.add(sqlCase);
        
        sqlCase = new SQLCase("select \"a\" from \"b.c\" where \"d\" = 'e' and \"f\" = 5 limit 10,100");
        sqlCase.setExpectedResult(new SQLCaseConf(SQLSanitizer.ENABLED_AUTO), "select ? from ? where ? = ? and ? = ? limit ?,?");
        sqlCase.setExpectedResult(new SQLCaseConf(SQLSanitizer.ENABLED_DROP_DOUBLE_QUOTED), "select ? from ? where ? = ? and ? = ? limit ?,?");
        sqlCase.setExpectedResult(new SQLCaseConf(SQLSanitizer.ENABLED_KEEP_DOUBLE_QUOTED), "select \"a\" from \"b.c\" where \"d\" = ? and \"f\" = ? limit ?,?");
        TEST_CASES.add(sqlCase);
        
       
        sqlCase = new SQLCase("select `a2a` from `b2b`.`c2c` where `d2d` = `e2e` limit 10,100");
        sqlCase.setExpectedResult(new SQLCaseConf(SQLSanitizer.ENABLED_AUTO), "select `a2a` from `b2b`.`c2c` where `d2d` = `e2e` limit ?,?");
        sqlCase.setExpectedResult(new SQLCaseConf(SQLSanitizer.ENABLED_DROP_DOUBLE_QUOTED), "select `a2a` from `b2b`.`c2c` where `d2d` = `e2e` limit ?,?");
        sqlCase.setExpectedResult(new SQLCaseConf(SQLSanitizer.ENABLED_KEEP_DOUBLE_QUOTED), "select `a2a` from `b2b`.`c2c` where `d2d` = `e2e` limit ?,?");
        TEST_CASES.add(sqlCase);
        
        sqlCase = new SQLCase("select \"a2a\" from \"b2b\".\"c2c\" where \"d2d\" = \"e2e\" limit 10,100");
        sqlCase.setExpectedResult(new SQLCaseConf(SQLSanitizer.ENABLED_AUTO), "select ? from ?.? where ? = ? limit ?,?");
        sqlCase.setExpectedResult(new SQLCaseConf(SQLSanitizer.ENABLED_DROP_DOUBLE_QUOTED), "select ? from ?.? where ? = ? limit ?,?");
        sqlCase.setExpectedResult(new SQLCaseConf(SQLSanitizer.ENABLED_KEEP_DOUBLE_QUOTED), "select \"a2a\" from \"b2b\".\"c2c\" where \"d2d\" = \"e2e\" limit ?,?");
        TEST_CASES.add(sqlCase);
        
        sqlCase = new SQLCase("INSERT INTO suppliers(col1,col2,col3) SELECT col1,col2,col3 FROM suppliers_backup where col1 = 'value';");
        sqlCase.setExpectedResult(new SQLCaseConf(SQLSanitizer.ENABLED_AUTO), "INSERT INTO suppliers(col1,col2,col3) SELECT col1,col2,col3 FROM suppliers_backup where col1 = ?;");
        sqlCase.setExpectedResult(new SQLCaseConf(SQLSanitizer.ENABLED_DROP_DOUBLE_QUOTED), "INSERT INTO suppliers(col1,col2,col3) SELECT col1,col2,col3 FROM suppliers_backup where col1 = ?;");
        sqlCase.setExpectedResult(new SQLCaseConf(SQLSanitizer.ENABLED_KEEP_DOUBLE_QUOTED), "INSERT INTO suppliers(col1,col2,col3) SELECT col1,col2,col3 FROM suppliers_backup where col1 = ?;");
        TEST_CASES.add(sqlCase);
        
        sqlCase = new SQLCase("CREATE TABLE suppliers AS (SELECT * FROM companies WHERE id > 1000);");
        sqlCase.setExpectedResult(new SQLCaseConf(SQLSanitizer.ENABLED_AUTO), "CREATE TABLE suppliers AS (SELECT * FROM companies WHERE id > ?);");
        sqlCase.setExpectedResult(new SQLCaseConf(SQLSanitizer.ENABLED_DROP_DOUBLE_QUOTED), "CREATE TABLE suppliers AS (SELECT * FROM companies WHERE id > ?);");
        sqlCase.setExpectedResult(new SQLCaseConf(SQLSanitizer.ENABLED_KEEP_DOUBLE_QUOTED), "CREATE TABLE suppliers AS (SELECT * FROM companies WHERE id > ?);");
        TEST_CASES.add(sqlCase);
    }

    private static class SQLCase {
        private String sourceQuery;
        private Map<SQLCaseConf, String> expectedResults = new LinkedHashMap<SQLCaseConf, String>();
        
        private SQLCase(String sourceQuery) {
            this.sourceQuery = sourceQuery;
        }
        
        private void setExpectedResult(SQLCaseConf conf, String expectedQuery) {
            expectedResults.put(conf, expectedQuery);
        }
    }
    
    private static class SQLCaseConf {
        private int sanitizeFlag;
        private String className;
        
        private SQLCaseConf(int flag) {
            sanitizeFlag = flag;
            className = "";
        }
        
        private SQLCaseConf(int flag, String name) {
            sanitizeFlag = flag;
            className = name;
        }
    }
   
}

