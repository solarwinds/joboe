package com.tracelytics.test.action;

import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.convention.annotation.Results;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
   
@Results({
    @Result(name="success", location="index.jsp"),
    @Result(name="error", location="index.jsp"),
})
public class TestSessionWithKeyspace extends AbstractCqlAction {
    
    @Override
    protected Session getSession() {
        return cluster.connect(TEST_KEYSPACE);
    }
    
    @Override
    protected String test(Session session) {
        
        String query = "SELECT * FROM " + TEST_TABLE_SIMPLE + " WHERE artist = ? AND title = ?  ALLOW FILTERING;";
        
        ResultSet set = session.execute(session.prepare(query).bind("Grumpy Cat", "Hi World"));
        printToOutput(query, set);
        
//        set = session.executeAsync(query,  "Grumpy Cat", "Hi World").getUninterruptibly();
//        printToOutput(query + " (async)", set);
        
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
