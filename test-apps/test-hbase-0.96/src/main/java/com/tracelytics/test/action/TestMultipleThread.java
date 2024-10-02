package com.tracelytics.test.action;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Result;
import org.apache.struts2.convention.annotation.Results;


@Results({
    @org.apache.struts2.convention.annotation.Result(name="success", location="index.jsp"),
    @org.apache.struts2.convention.annotation.Result(name="error", location="index.jsp"),
    @org.apache.struts2.convention.annotation.Result(name="input", location="index.jsp")
})
public class TestMultipleThread extends AbstractHBaseTest {
    
    public String execute() throws Exception {
        
        
        final HTable table = getTable(TABLE_TEST);
        
        
        final List<Get> gets = new ArrayList<Get>();
        //Result[] results = table.get(Arrays.asList(new Get("row1".getBytes()), new Get("row2".getBytes()), new Get("row3".getBytes())));
        for (int i = 0 ; i < 100000; i ++) {
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
            
        addActionMessage("Get(List) Multiple thread is successful");
        
        return SUCCESS;
    }
}
