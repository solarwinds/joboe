package com.tracelytics.instrumentation.jdbc;

import java.lang.reflect.Field;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.tracelytics.AnyValueValidator;
import com.tracelytics.ExpectedEvent;
import com.tracelytics.agent.Agent;
import com.tracelytics.instrumentation.AbstractInstrumentationTest;
import com.tracelytics.joboe.config.ConfigContainer;
import com.tracelytics.joboe.config.ConfigManager;
import com.tracelytics.joboe.config.ConfigProperty;
import com.tracelytics.joboe.config.InvalidConfigException;


public class StatementInstrumentationTest extends AbstractInstrumentationTest<StatementInstrumentation> {

    public StatementInstrumentationTest() throws SecurityException, IllegalArgumentException, IllegalAccessException, NoSuchFieldException, InvalidConfigException {
        super();
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        ConfigManager.setConfig(ConfigProperty.AGENT_SQL_SANITIZE, 0);
        ConfigManager.setConfig(ConfigProperty.AGENT_JDBC_INST_ALL, true);
    }

    @Override
    protected void tearDown() throws Exception {
        ConfigManager.reset();
        super.tearDown();
    }

    public void testNormalSql() throws Exception {
//        Statement statement = (Statement) createInstrumentedInstance("com.tracelytics.instrumentation.jdbc.StatementStub");
//        System.out.println(Agent.reporter());
        Statement statement = new StatementStub();
        
        String query = "SELECT * FROM foo";
        statement.execute(query);
        
        List<ExpectedEvent> expectedEvents = new ArrayList<ExpectedEvent>();
        
        
        
        ExpectedEvent entryEvent = new ExpectedEvent();
        entryEvent.addInfo("Layer", "jdbc_tracelytics");
        entryEvent.addInfo("Label", "entry");
        entryEvent.addInfo("JDBC-Method", "execute");
        entryEvent.addInfo("JDBC-Class", "com.tracelytics.instrumentation.jdbc.StatementStub");
        entryEvent.addInfoWithValidator("Backtrace", new AnyValueValidator());
        
        expectedEvents.add(entryEvent);
        
        ExpectedEvent exitEvent = new ExpectedEvent();
        exitEvent.addInfo("Layer", "jdbc_tracelytics");
        exitEvent.addInfo("Label", "exit");
        exitEvent.addInfo("Query", query);
        exitEvent.addInfo("Flavor", "tracelytics");
        
        expectedEvents.add(exitEvent);
        
        assertEvents(expectedEvents);
        
    }
    
    
    /**
    * Test on long SQL that may trigger BufferOverflowException as reported in https://github.com/tracelytics/joboe/issues/55
    */

    public void testLongSql() throws Exception {
//        StatementInstrumentation.layerExecuteEntry(LAYER_NAME, statement, "execute");
        int length = 1000000;

        char[] longCharacterArray = new char[length];

        for (int i = 0; i < length; i++) {
            //longCharacterArray[i] = 37109; //front end seems to be having issues with UTF-8 characters that are truncated. refer to UtfSQL test cases below.
            longCharacterArray[i] = 80;
        }
        
        Statement statement = new StatementStub();
        
        String query = "INSERT INTO foo(comments) VALUES ('" + new String(longCharacterArray) + "')";
        statement.execute(query);
        
        List<ExpectedEvent> expectedEvents = new ArrayList<ExpectedEvent>();
        
        
        
        ExpectedEvent entryEvent = new ExpectedEvent();
        entryEvent.addInfo("Layer", "jdbc_tracelytics");
        entryEvent.addInfo("Label", "entry");
        entryEvent.addInfo("JDBC-Method", "execute");
        entryEvent.addInfo("JDBC-Class", "com.tracelytics.instrumentation.jdbc.StatementStub");
        entryEvent.addInfoWithValidator("Backtrace", new AnyValueValidator());
        
        expectedEvents.add(entryEvent);
        
        ExpectedEvent exitEvent = new ExpectedEvent();
        exitEvent.addInfo("Layer", "jdbc_tracelytics");
        exitEvent.addInfo("Label", "exit");
        exitEvent.addInfo("Query", query.substring(0, StatementInstrumentation.DEFAULT_SQL_MAX_LENGTH));
        exitEvent.addInfo("QueryTruncated", true);
        exitEvent.addInfo("Flavor", "tracelytics");
        
        expectedEvents.add(exitEvent);
        
        assertEvents(expectedEvents);

//        StatementInstrumentation.layerExecuteExit(LAYER_NAME, "mysql", statement, "execute", "INSERT INTO foo(comments) VALUES ('" + new String(longCharacterArray) + "')", false);
    }

    /**
     * This test case does not work (on front-end) as the SQL sent down does not close with ' , combined with unicode, it does not work well. 
     * Take note the size is 100, so it has nothing to do with truncation 
     * @throws Exception 
     */
    public void testUnicodeSqlWithoutEndingQuote() throws Exception {
//        StatementInstrumentation.layerExecuteEntry(LAYER_NAME, statement, "execute");
        int length = 100;

        char[] longCharacterArray = new char[length];

        for (int i = 0; i < length; i++) {
            longCharacterArray[i] = 37109; //front end seems to be having issues with UTF-8 characters that are truncated
        }
        
        Statement statement = new StatementStub();
        
        String query = "INSERT INTO foo(comments) VALUES ('" + new String(longCharacterArray) + "')";
        statement.execute(query);
        
        List<ExpectedEvent> expectedEvents = new ArrayList<ExpectedEvent>();
        
        ExpectedEvent entryEvent = new ExpectedEvent();
        entryEvent.addInfo("Layer", "jdbc_tracelytics");
        entryEvent.addInfo("Label", "entry");
        entryEvent.addInfo("JDBC-Method", "execute");
        entryEvent.addInfo("JDBC-Class", "com.tracelytics.instrumentation.jdbc.StatementStub");
        entryEvent.addInfoWithValidator("Backtrace", new AnyValueValidator());
        
        expectedEvents.add(entryEvent);
        
        ExpectedEvent exitEvent = new ExpectedEvent();
        exitEvent.addInfo("Layer", "jdbc_tracelytics");
        exitEvent.addInfo("Label", "exit");
        exitEvent.addInfo("Query", query);
        exitEvent.addInfo("Flavor", "tracelytics");
        
        expectedEvents.add(exitEvent);
        
        assertEvents(expectedEvents);
        

//        StatementInstrumentation.layerExecuteExit(LAYER_NAME, "mysql", statement, "execute", "INSERT INTO foo(comments) VALUES ('" + new String(longCharacterArray), false);
    }

    /**
     * This test case works as the sql sent down closes with ' (a complete query)
     * @throws Exception 
     */
    public void testUnicodeSqlWithEndingQuote() throws Exception {
//        StatementInstrumentation.layerExecuteEntry(LAYER_NAME, statement, "execute");
        int length = 100;

        char[] longCharacterArray = new char[length];

        for (int i = 0; i < length; i++) {
            longCharacterArray[i] = 37109; //front end seems to be having issues with UTF-8 characters that are truncated
        }
        
        Statement statement = new StatementStub();
        
        String query = "INSERT INTO foo(comments) VALUES ('" + new String(longCharacterArray); //WITHOUT the ending ') !!!
        statement.execute(query);
        
        List<ExpectedEvent> expectedEvents = new ArrayList<ExpectedEvent>();
        
        ExpectedEvent entryEvent = new ExpectedEvent();
        entryEvent.addInfo("Layer", "jdbc_tracelytics");
        entryEvent.addInfo("Label", "entry");
        entryEvent.addInfo("JDBC-Method", "execute");
        entryEvent.addInfo("JDBC-Class", "com.tracelytics.instrumentation.jdbc.StatementStub");
        entryEvent.addInfoWithValidator("Backtrace", new AnyValueValidator());
        
        expectedEvents.add(entryEvent);
        
        ExpectedEvent exitEvent = new ExpectedEvent();
        exitEvent.addInfo("Layer", "jdbc_tracelytics");
        exitEvent.addInfo("Label", "exit");
        exitEvent.addInfo("Query", query);
        exitEvent.addInfo("Flavor", "tracelytics");
        
        expectedEvents.add(exitEvent);
        
        assertEvents(expectedEvents);

        //StatementInstrumentation.layerExecuteExit(LAYER_NAME, "mysql", statement, "execute", "INSERT INTO foo(comments) VALUES ('" + new String(longCharacterArray) + "')", false);
    }
}
