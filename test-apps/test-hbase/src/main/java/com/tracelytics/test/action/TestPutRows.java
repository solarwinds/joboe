package com.tracelytics.test.action;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.BufferedMutator;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.client.Put;
import org.apache.struts2.convention.annotation.Results;

import com.opensymphony.xwork2.ActionSupport;


@Results({
    @org.apache.struts2.convention.annotation.Result(name="success", location="index.jsp"),
    @org.apache.struts2.convention.annotation.Result(name="error", location="index.jsp"),
    @org.apache.struts2.convention.annotation.Result(name="input", location="index.jsp")
})
public class TestPutRows extends AbstractHBaseTest {
    private int count;
    
    
    
    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public String execute() throws Exception {
        BufferedMutator mutator = getMutator(TABLE_TEST);
        
        testPut(mutator);
        
        
        //now flush the commits here
        mutator.flush();
        
        addActionMessage("Put [" + count + "] row(s)");
        
        return SUCCESS;
    }
    
    public void testPut(BufferedMutator mutator) throws IOException {
        List<Put> puts = new ArrayList<Put>();
        for (int i = 0 ; i < count; i ++) {
            puts.add(new Put(("small-" + i).getBytes()).add(CF_CF, COLUMN_A, String.valueOf(i).getBytes()));
        }
        
        mutator.mutate(puts);
    }
}
