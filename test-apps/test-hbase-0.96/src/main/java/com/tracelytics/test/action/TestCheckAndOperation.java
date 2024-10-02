package com.tracelytics.test.action;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Put;
import org.apache.struts2.convention.annotation.Results;

import com.opensymphony.xwork2.ActionSupport;


@Results({
    @org.apache.struts2.convention.annotation.Result(name="success", location="index.jsp"),
    @org.apache.struts2.convention.annotation.Result(name="error", location="index.jsp"),
    @org.apache.struts2.convention.annotation.Result(name="input", location="index.jsp")
})
public class TestCheckAndOperation extends AbstractHBaseTest {
    
    public String execute() throws Exception {
        HTable table = getTable(TABLE_TEST);
        
        table.checkAndPut(ROW_TEST, CF_BLOB, "temp".getBytes(), null, new Put(ROW_TEST).add(CF_BLOB, "temp".getBytes(), "tempValue".getBytes()));
        table.checkAndDelete(ROW_TEST, CF_BLOB, "temp".getBytes(), "tempValue".getBytes(), new Delete(ROW_TEST));
        
        
        addActionMessage("CheckAndPut and CheckAndDelete are successful");
        
        return SUCCESS;
    }
}
