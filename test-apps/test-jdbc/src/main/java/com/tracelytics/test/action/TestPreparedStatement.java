package com.tracelytics.test.action;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.Map;

import org.apache.struts2.convention.annotation.ParentPackage;

@SuppressWarnings("serial")
@org.apache.struts2.convention.annotation.Results({
    @org.apache.struts2.convention.annotation.Result(name="success", type="json"),
    @org.apache.struts2.convention.annotation.Result(name="error", type="json"),
})
@ParentPackage("json-default")
public class TestPreparedStatement extends AbstractStatementAction {
    @Override
    protected ResultSet executeStatement(Connection connection)
        throws Exception {
        String query = queryForm.getQuery();
        String parameters = queryForm.getParameters();
        PreparedStatement statement = connection.prepareStatement(query);
        int parameterIndex = 1;
        for (String parameter : parameters.split(" ")) {
            statement.setObject(parameterIndex++, parameter);
        }
        
        statement.execute();
        
        return statement.getResultSet();
    }
    
    @Override
    public List<Map<String, Object>> getColumns() {
        // TODO Auto-generated method stub
        return super.getColumns();
    }
    
    @Override
    public List<Map<String, Object>> getData() {
        // TODO Auto-generated method stub
        return super.getData();
    }
    
    public String getMessage() {
        return message;
    }
}
