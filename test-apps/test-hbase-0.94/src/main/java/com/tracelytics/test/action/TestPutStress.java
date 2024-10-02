package com.tracelytics.test.action;

import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Put;
import org.apache.struts2.convention.annotation.Results;


@Results({
    @org.apache.struts2.convention.annotation.Result(name="success", location="index.jsp"),
    @org.apache.struts2.convention.annotation.Result(name="error", location="index.jsp"),
    @org.apache.struts2.convention.annotation.Result(name="input", location="index.jsp")
})
public class TestPutStress extends AbstractTestStress {
    
    public String execute() throws Exception {
        HTable table = getTable(TABLE_TEST);
        
        Put putParam = new Put(ROW_NAME).add(CF_CF, COLUMN_A, "stress".getBytes());
        for (int i = 0 ; i < getRunCount(); i++) {
            table.put(putParam);
            
        }
        
        addActionMessage("Put was run [" + getRunCount() + "] time(s)");
        
        return SUCCESS;
    }
}
