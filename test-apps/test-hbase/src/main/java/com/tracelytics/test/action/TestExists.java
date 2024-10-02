package com.tracelytics.test.action;

import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Table;
import org.apache.struts2.convention.annotation.Results;


@Results({
    @org.apache.struts2.convention.annotation.Result(name="success", location="index.jsp"),
    @org.apache.struts2.convention.annotation.Result(name="error", location="index.jsp"),
    @org.apache.struts2.convention.annotation.Result(name="input", location="index.jsp")
})
public class TestExists extends AbstractHBaseTest {
    private static final byte[] TEMP = "temp".getBytes();
    
    
    public String execute() throws Exception {
        Table table = getTable(TABLE_TEST);
        
        table.exists(new Get("row1".getBytes()).addFamily(CF_CF));
        
        addActionMessage("Exists is successful");
        
        return SUCCESS;
    }
}
