package com.tracelytics.test.action;

import java.util.UUID;

import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.convention.annotation.Results;

import com.datastax.driver.core.Session;
   
@Results({
    @Result(name="success", location="index.jsp"),
    @Result(name="error", location="index.jsp"),
})
public class TestUpdate extends AbstractCqlAction {
    @Override
    protected String test(Session session) {
        session.execute(session.prepare("UPDATE " + TEST_TABLE + " SET data = ? WHERE id = ?").bind(null, UUID.randomUUID()));
       
        addActionMessage("Query executed successfully");
        return SUCCESS;
    }
    
    
  
}
