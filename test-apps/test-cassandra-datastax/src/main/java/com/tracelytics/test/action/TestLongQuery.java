package com.tracelytics.test.action;

import java.util.UUID;

import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.convention.annotation.Results;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
   
@SuppressWarnings("serial")
@Results({
    @Result(name="success", location="index.jsp"),
    @Result(name="error", location="index.jsp"),
})
public class TestLongQuery extends AbstractCqlAction {
    public static final int COUNT = 1000;
    
    @Override
    protected String test(Session session) {
        StringBuffer statement = new StringBuffer();
        
        statement.append("BEGIN BATCH ");
        
        for (int i = 0; i < COUNT; i ++) {
            statement.append(
                            "INSERT INTO " + TEST_TABLE + " (id, title, album, artist, tags) " +
                            "VALUES (" +
                                UUID.randomUUID().toString() + ',' +
                                "'title " + i + "'," +
                                "'xyz'," +
                                "'Fruitful Singer'," +
                                "{}) ");
                                
        }
        
        statement.append("APPLY BATCH;");
        
        session.execute(statement.toString());
        
        addActionMessage("Query executed successfully (inserted [" + COUNT +"] records)");
    
        return SUCCESS;
    }
}
