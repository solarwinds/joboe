package com.tracelytics.test.action;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
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
public class TestBulkGet extends AbstractHBaseTest {
    
    public String execute() throws Exception {
        
        
        HTable table = getTable(TABLE_TEST);
        
        List<Get> gets = new ArrayList<Get>();
        //Result[] results = table.get(Arrays.asList(new Get("row1".getBytes()), new Get("row2".getBytes()), new Get("row3".getBytes())));
        for (int i = 0 ; i < 30; i ++) {
            Get get = new Get(("big-" + i).getBytes());
            gets.add(get);
        }
        
        Result[] results = table.get(gets);
        
        addActionMessage("Get (multi/bulk) is successful");
        
        return SUCCESS;
    }
}
