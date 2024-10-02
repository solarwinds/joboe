package com.tracelytics.test.action;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.struts2.convention.annotation.Results;

import com.opensymphony.xwork2.ActionSupport;


@Results({
    @org.apache.struts2.convention.annotation.Result(name="success", location="index.jsp"),
    @org.apache.struts2.convention.annotation.Result(name="error", location="index.jsp"),
    @org.apache.struts2.convention.annotation.Result(name="input", location="index.jsp")
})
public class TestDelete extends AbstractHBaseTest {
    private static final byte[] TEMP = "temp".getBytes();
    
    
    public String execute() throws Exception {
        HTable table = getTable(TABLE_TEST);
        
        table.put(new Put(TEMP).add(CF_CF, TEMP, TEMP));
        
        table.delete(new Delete(TEMP).deleteColumn(CF_CF, TEMP));
        
        table.delete(new Delete(TEMP).deleteFamily(CF_CF));
        
        table.delete(new Delete(TEMP));
        
        List<Delete> deletes = new ArrayList<Delete>();
        
        deletes.add(new Delete(TEMP));
        deletes.add(new Delete(TEMP));
        deletes.add(new Delete(TEMP));
        
        table.delete(deletes);
        
        addActionMessage("Delete is successful");
        
        return SUCCESS;
    }
}
