package com.tracelytics.test.action;

import org.apache.struts2.convention.annotation.ParentPackage;

import javax.sql.rowset.serial.SerialBlob;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.Map;

@SuppressWarnings("serial")
@org.apache.struts2.convention.annotation.Results({
    @org.apache.struts2.convention.annotation.Result(name="success", type="json"),
    @org.apache.struts2.convention.annotation.Result(name="error", type="json"),
})
@ParentPackage("json-default")
public class TestBlobStatement extends AbstractStatementAction {
    @Override
    protected ResultSet executeStatement(Connection connection)
        throws Exception {
        PreparedStatement statement = connection.prepareStatement("SELECT * FROM " + TEST_TABLE + " WHERE b = ?");
        Blob blob = new SerialBlob("".getBytes());
        statement.setBlob(1, blob);

        statement.execute();
        
        return statement.getResultSet();
    }
    
    @Override
    public List<Map<String, Object>> getColumns() {
        return super.getColumns();
    }
    
    @Override
    public List<Map<String, Object>> getData() {
        return super.getData();
    }
    
    public String getMessage() {
        return message;
    }
}
