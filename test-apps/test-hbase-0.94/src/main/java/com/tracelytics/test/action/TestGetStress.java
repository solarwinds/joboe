package com.tracelytics.test.action;

import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.struts2.convention.annotation.Results;


@Results({
    @org.apache.struts2.convention.annotation.Result(name="success", location="index.jsp"),
    @org.apache.struts2.convention.annotation.Result(name="error", location="index.jsp"),
    @org.apache.struts2.convention.annotation.Result(name="input", location="index.jsp")
})
public class TestGetStress extends AbstractTestStress {
    
    public String execute() throws Exception {
        HTable table = getTable(TABLE_TEST);
        
        Get getParam = new Get(ROW_NAME);
        for (int i = 0 ; i < getRunCount(); i++) {
            table.get(getParam);
        }
        
        addActionMessage("Get was run [" + getRunCount() + "] time(s)");
        
        return SUCCESS;
    }
}
