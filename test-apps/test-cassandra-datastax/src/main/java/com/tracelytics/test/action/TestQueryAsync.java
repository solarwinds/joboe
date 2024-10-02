package com.tracelytics.test.action;

import java.util.concurrent.ExecutionException;

import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.convention.annotation.Results;

import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.SimpleStatement;
   
@SuppressWarnings("serial")
@Results({
    @Result(name="success", location="index.jsp"),
    @Result(name="error", location="index.jsp"),
})
public class TestQueryAsync extends AbstractCqlAction {
    @Override
    protected String test(Session session) throws InterruptedException, ExecutionException {
        ResultSetFuture set1 = session.executeAsync("SELECT * FROM " + TEST_TABLE);
        
        
        String query = "SELECT * FROM " + TEST_TABLE + " WHERE artist = ? AND title = ? ALLOW FILTERING;";
        
        ResultSetFuture set2 = session.executeAsync(query, "Grumpy Cat", "Hi World");
        
        ResultSetFuture set3 = session.executeAsync(new SimpleStatement(query, "Grumpy Cat", "Hi World"));
               
        PreparedStatement preparedStatement = session.prepare(query);
        preparedStatement.setConsistencyLevel(ConsistencyLevel.ONE);
        ResultSetFuture set4 = session.executeAsync(preparedStatement.bind("Grumpy Cat", "Hi World"));
        
        printToOutput("excuteAsync(String)", set1.getUninterruptibly());
        printToOutput("excuteAsync(String, String...)", set2.getUninterruptibly());
        printToOutput("excuteAsync(SimpleStatement)", set3.getUninterruptibly());
        printToOutput("excuteAsync(BoundStatement)", set4.getUninterruptibly());
        
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
}
