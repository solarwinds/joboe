package com.tracelytics.test.action;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ParameterMetaData;
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
public class TestCallableStatement extends AbstractStatementAction {
    @Override
    protected ResultSet executeStatement(Connection connection)
        throws Exception {
        String query = queryForm.getQuery();
        CallableStatement statement = connection.prepareCall(query);

        String[] parameters = queryForm.getParameters().split(" ");
        int parametersWalker = 0;
        for (int i = 0 ; i < statement.getParameterMetaData().getParameterCount(); i ++) {
            int parameterMode = statement.getParameterMetaData().getParameterMode(i + 1);
            int parameterType = statement.getParameterMetaData().getParameterType(i + 1);
            if (parameterMode == ParameterMetaData.parameterModeIn || parameterMode == ParameterMetaData.parameterModeInOut) {
                if (parametersWalker == parameters.length) { //not enough parameter
                    throw new IllegalArgumentException("Not enough bind parameters, failed to bind on IN parameter at [" + (i + 1) + "]" );
                }
                statement.setObject(i + 1, parameters[parametersWalker ++]);
            }
            if (parameterMode == ParameterMetaData.parameterModeOut || parameterMode == ParameterMetaData.parameterModeInOut) {
                statement.registerOutParameter(i + 1, parameterType);
            }
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
