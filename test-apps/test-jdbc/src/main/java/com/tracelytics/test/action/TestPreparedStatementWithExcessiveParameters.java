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
public class TestPreparedStatementWithExcessiveParameters extends AbstractStatementAction {
    private static final int PARAMETER_COUNT = 10000;
    private static final String QUERY;
    static {
        StringBuffer query = new StringBuffer("SELECT * FROM test_table WHERE last_name IN (");
        for (int i = 0; i < PARAMETER_COUNT - 1; i++) {
            query.append("?, ");
        }
        query.append("?)");
        QUERY = query.toString();
    }
    
    
    @Override
    protected ResultSet executeStatement(Connection connection)
        throws Exception {
        
        PreparedStatement statement = connection.prepareStatement(QUERY);
        for (int i = 0 ; i < PARAMETER_COUNT; i ++) {
            statement.setString(i + 1, String.valueOf(i));
        }
        
        statement.execute();
        
        message = "Executed prepared statement with [" + PARAMETER_COUNT + "] parameters";
        
        return statement.getResultSet();
    }
    
    @Override
    /**
     * just need to declare this so the json response includes this
     * @return
     */
    public List<Map<String, Object>> getColumns() {
        // TODO Auto-generated method stub
        return super.getColumns();
    }
    
    @Override
    /**
     * just need to declare this so the json response includes this
     * @return
     */
    public List<Map<String, Object>> getData() {
        // TODO Auto-generated method stub
        return super.getData();
    }
    
    /**
     * just need to declare this so the json response includes this
     * @return
     */
    public String getMessage() {
        return message;
    }
}
