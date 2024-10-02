package com.tracelytics.test.action;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Put;
import org.apache.struts2.convention.annotation.Results;

import com.opensymphony.xwork2.ActionSupport;


@Results({
    @org.apache.struts2.convention.annotation.Result(name="success", location="index.jsp"),
    @org.apache.struts2.convention.annotation.Result(name="error", location="index.jsp"),
    @org.apache.struts2.convention.annotation.Result(name="input", location="index.jsp")
})
public class TestBigPut extends AbstractHBaseTest {
    
    public String execute() throws Exception {
        
        HTable table = getTable(TABLE_TEST);
        
        table.setAutoFlush(false);
        
        testPut(table);
        
        table.flushCommits();
        
        addActionMessage("Put (batching) is successful");
        
        return SUCCESS;
    }
    
    public void testPut(HTable table) throws IOException {
        List<Put> puts = new ArrayList<Put>();
        
        
        for (int i = 0 ; i < 30; i ++) {
            Put put = new Put(("big-" + (i + 30)).getBytes());
            put.add(CF_CF, COLUMN_A, String.valueOf(i).getBytes());
            put.add(CF_BLOB, COLUMN_DATA, new byte[1024 * 1024]);
            puts.add(put);
        }
        
        table.put(puts);
    }
}
