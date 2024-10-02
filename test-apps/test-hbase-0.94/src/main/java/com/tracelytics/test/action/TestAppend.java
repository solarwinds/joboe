package com.tracelytics.test.action;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Append;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.struts2.convention.annotation.Results;

import com.opensymphony.xwork2.ActionSupport;


@Results({
    @org.apache.struts2.convention.annotation.Result(name="success", location="index.jsp"),
    @org.apache.struts2.convention.annotation.Result(name="error", location="index.jsp"),
    @org.apache.struts2.convention.annotation.Result(name="input", location="index.jsp")
})
public class TestAppend extends AbstractHBaseTest {
    
    public String execute() throws Exception {
        
        HTable table = getTable(TABLE_TEST);
        
        table.append(new Append(ROW_TEST).add(CF_BLOB, COLUMN_DATA, "x".getBytes()));
        
        addActionMessage("Append is successful");
        
        return SUCCESS;
    }
}
