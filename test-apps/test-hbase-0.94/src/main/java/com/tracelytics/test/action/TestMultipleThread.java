package com.tracelytics.test.action;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.struts2.convention.annotation.Results;

@Results({
    @org.apache.struts2.convention.annotation.Result(name="success", location="index.jsp"),
    @org.apache.struts2.convention.annotation.Result(name="error", location="index.jsp"),
    @org.apache.struts2.convention.annotation.Result(name="input", location="index.jsp")
})
public class TestMultipleThread extends AbstractHBaseTest {
    private static final int DATA_SET_SIZE = 100000;
    private static final int BATCH_SIZE = 20000;
    public String execute() throws Exception {
        
        
        final HTable table = getTable(TABLE_TEST);
        // PUTs
        
        //Result[] results = table.get(Arrays.asList(new Get("row1".getBytes()), new Get("row2".getBytes()), new Get("row3".getBytes())));
        
        final List<List<Put>> batches = new ArrayList<List<Put>>();
        
        List<Put> puts = null;
        for (int i = 0 ; i < DATA_SET_SIZE; i ++) {
            if (i % BATCH_SIZE == 0) { //create a new put batch
                puts = new ArrayList<Put>();
                batches.add(puts);
            }
            
            Put put = new Put(("small-" + i).getBytes()).add(CF_CF, COLUMN_A, String.valueOf(i).getBytes());
            puts.add(put);
        }
        
        for (final List<Put> batch : batches) {
            Thread thread = new Thread() {
                public void run() {
                    try {
                        table.put(batch);
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
                
            };
            thread.start();
            thread.join();
        }
        
        
        
        // GETs
        final List<Get> gets = new ArrayList<Get>();
        //Result[] results = table.get(Arrays.asList(new Get("row1".getBytes()), new Get("row2".getBytes()), new Get("row3".getBytes())));
        for (int i = 0 ; i < DATA_SET_SIZE; i ++) {
            Get get = new Get(("small-" + i).getBytes());
            gets.add(get);
        }
        
        
        for (int i = 0 ; i < 5; i ++) {
            Thread thread = new Thread() {
                public void run() {
                    try {
                        Result[] results = table.get(gets);
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
                
            };
            thread.start();
            thread.join();
        }
            
        addActionMessage("Get (multi/bulk) is successful");
        
        return SUCCESS;
    }
}
