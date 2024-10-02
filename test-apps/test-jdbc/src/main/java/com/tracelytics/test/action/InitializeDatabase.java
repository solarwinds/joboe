package com.tracelytics.test.action;

import java.sql.Connection;
import java.sql.SQLException;

@SuppressWarnings("serial")

@org.apache.struts2.convention.annotation.Results({
    @org.apache.struts2.convention.annotation.Result(name="success", location="index.jsp"),
    @org.apache.struts2.convention.annotation.Result(name="error", location="index.jsp"),
    @org.apache.struts2.convention.annotation.Result(name="input", location="index.jsp"),
})
public class InitializeDatabase extends AbstractJdbcAction {

    @Override
    public String execute() throws Exception {
        try {
            if (initializeDb()) {
                session.put("isInitialized", true);
                printToOutput("Initialized database " + getConnectionString() + " testing table is [" + TEST_TABLE + "]");
                return SUCCESS;
            } else {
                printToOutput("Failed to initialize database " + getConnectionString());
                return ERROR;
            }
        } catch (FormInputException e) {
            addFieldError(e.getField(), e.getMessage());
            return INPUT;
        }
    }
    
    
    
    @Override
    protected String execute(Connection connection) throws SQLException {
        return SUCCESS;
    }

}
