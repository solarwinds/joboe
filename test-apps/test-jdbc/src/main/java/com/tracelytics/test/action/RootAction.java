package com.tracelytics.test.action;

import java.sql.Connection;
import java.sql.SQLException;

import org.apache.struts2.convention.annotation.Action;

@SuppressWarnings("serial")

@Action("")
@org.apache.struts2.convention.annotation.Results({
    @org.apache.struts2.convention.annotation.Result(name="success", location="index.jsp"),
    @org.apache.struts2.convention.annotation.Result(name="error", location="index.jsp"),
    @org.apache.struts2.convention.annotation.Result(name="input", location="index.jsp"),
})
public class RootAction extends AbstractJdbcAction {

    @Override
    public String execute() throws Exception {
        return SUCCESS;
    }
    
    @Override
    protected String execute(Connection connection) throws SQLException {
        return SUCCESS;
    }

}
