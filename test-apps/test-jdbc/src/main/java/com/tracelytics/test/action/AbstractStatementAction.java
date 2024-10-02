package com.tracelytics.test.action;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractStatementAction extends AbstractJdbcAction {
    protected QueryForm queryForm;
    private List<Map<String, Object>> columns;
    private List<Map<String, Object>> data;
    protected String message;
    
    @Override
    protected String execute(Connection connection) throws Exception {
        try {
            resetData();
            ResultSet result = executeStatement(connection);
            if (result != null) {
                int columnCount = result.getMetaData().getColumnCount();
        
                
                for (int i = 0 ; i < columnCount; i++) {
                    String columnName = result.getMetaData().getColumnName(i + 1);
                    columns.add(getHeader(columnName, i));
                }
                
                
                while (result.next()) {
                    Map<String, Object> rowValues = new HashMap<String, Object>();
                    data.add(rowValues);
                    for (int i = 0 ; i < columnCount; i++) {
                        Object o = result.getObject(i + 1);
                        String value = o != null ? o.toString() : "(null)";
                        rowValues.put(String.valueOf(i), value);
                    }
                }
            }
            
            if (queryForm != null && queryForm.getQuery() != null) {
                message = "Executed [" + queryForm.getQuery() + "]";
            }
            
    //        setExtendedOutput(resultList);
            return SUCCESS;
        } catch (Exception e) {
            e.printStackTrace();
            message = e.getMessage();
            return ERROR;
        }
        
    }
    
    protected abstract ResultSet executeStatement(Connection connection) throws Exception;

    public List<Map<String, Object>> getData() {
        return data;
    }
    
    public List<Map<String, Object>> getColumns() {
        return columns;
    }
    
    protected void resetData() {
        columns = new ArrayList<Map<String, Object>>();
        data = new ArrayList<Map<String, Object>>();
    }
    
    private Map<String, Object> getHeader(String columnName, int columnIndex) {
        Map<String, Object> header = new HashMap<String, Object>();
        header.put("sTitle", columnName);
        header.put("mData", String.valueOf(columnIndex));
        header.put("aTargets", Collections.singletonList(columnIndex));
        return header;
    }
    
    public QueryForm getQueryForm() {
        return queryForm;
    }
    public void setQueryForm(QueryForm queryForm) {
        this.queryForm = queryForm;
    }   
}
