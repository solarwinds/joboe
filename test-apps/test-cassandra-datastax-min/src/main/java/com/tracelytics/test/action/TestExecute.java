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
public class TestExecute extends AbstractCqlAction {
    
    private String statement;
    
    @Override
    protected String test(Session session) {
        ResultSet set = session.execute(statement);
        
        printToOutput("excute(String)", set);
        
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


    public String getStatement() {
        return statement;
    }


    public void setStatement(String statement) {
        this.statement = statement;
    }
}
