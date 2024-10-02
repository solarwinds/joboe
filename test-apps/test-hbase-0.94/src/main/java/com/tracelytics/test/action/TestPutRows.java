package com.tracelytics.test.action;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Put;
import org.apache.struts2.convention.annotation.Results;

import com.opensymphony.xwork2.ActionSupport;


@Results({
    @org.apache.struts2.convention.annotation.Result(name="success", location="index.jsp"),
    @org.apache.struts2.convention.annotation.Result(name="error", location="index.jsp"),
    @org.apache.struts2.convention.annotation.Result(name="input", location="index.jsp")
})
public class TestPutRows extends AbstractHBaseTest {
    private static final byte[] ROW_KEY_PREFIX = new byte[] { (byte)0xff, (byte)0xfe, (byte)0xfd };
    
    private int count;
    
    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public String execute() throws Exception {
        
        HTable table = getTable(TABLE_TEST);
        
        table.setAutoFlush(false);
        
        testPut(table);
        
        
        //now flush the commits here
        table.flushCommits();
        
        addActionMessage("Put [" + count + "] row(s)");
        
        return SUCCESS;
    }
    
    public void testPut(HTable table) throws IOException {
        List<Put> puts = new ArrayList<Put>();
        for (int i = 0 ; i < count; i ++) {
            byte[] suffix = String.valueOf(i).getBytes();
            byte[] key = new byte[ROW_KEY_PREFIX.length + suffix.length];
            
            System.arraycopy(ROW_KEY_PREFIX, 0, key, 0, ROW_KEY_PREFIX.length);
            System.arraycopy(suffix, 0, key, ROW_KEY_PREFIX.length, suffix.length);
            
            puts.add(new Put(key).add(CF_CF, COLUMN_A, String.valueOf(i).getBytes()));
        }
        
        table.put(puts);
    }
}
