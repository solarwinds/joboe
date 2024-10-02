package com.tracelytics.test.action;

import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.convention.annotation.Results;

import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.SimpleStatement;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.QueryBuilder;
   
@SuppressWarnings("serial")
@Results({
    @Result(name="success", location="index.jsp"),
    @Result(name="error", location="index.jsp"),
})
public class TestQuery extends AbstractCqlAction {
    @Override
    protected String test(Session session) {
        ResultSet set = session.execute("SELECT * FROM " + TEST_TABLE);
        
        printToOutput("execute(String)", set);
        
        String query = "SELECT * FROM " + TEST_TABLE + " WHERE artist = ? AND title = ?  ALLOW FILTERING;";
        
        set = session.execute(query, "Grumpy Cat", "Hi World");
        printToOutput("execute(String, String...)", set);
        
        set = session.execute(new SimpleStatement(query, "Grumpy Cat", "Hi World"));
        printToOutput("execute(SimpleStatement)", set);
        
        set = session.execute(new MySimpleStatement(query, "Grumpy Cat", "Hi World"));
        printToOutput("execute(MySimpleStatement)", set);
        
        PreparedStatement preparedStatement = session.prepare(query);
        set = session.execute(preparedStatement.bind("Grumpy Cat", "Hi World"));
        printToOutput("execute(BoundStatement)", set);
        
        Statement statement = QueryBuilder.select().all().from(TEST_KEYSPACE, TEST_TABLE_SIMPLE).where(QueryBuilder.eq("artist", "Grumpy Cat"));
        statement.setConsistencyLevel(ConsistencyLevel.ONE);
        set = session.execute(statement);
        printToOutput("execute(Query)", set);
        
        addActionMessage("Query executed successfully");
        return SUCCESS;
    }
    
    
    private void printToOutput(String title, ResultSet set) {
        if (title != null) {
            appendExtendedOutput(title);
        }
        for (Row row : set) {
            appendExtendedOutput(row.toString());
        }
    }
    
    /**
     * Test custom class (make sure ctor modification works properly)
     * @author pluk
     *
     */
    private class MySimpleStatement extends SimpleStatement {
        public MySimpleStatement(String query, String...args) {
            super(query, args);
        }
    }
}
