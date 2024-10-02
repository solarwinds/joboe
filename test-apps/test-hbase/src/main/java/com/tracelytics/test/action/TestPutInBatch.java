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
public class TestPutInBatch extends AbstractHBaseTest {
    
    public String execute() throws Exception {
        BufferedMutator mutator = getMutator(TABLE_TEST);
        
        testPut(mutator);
        
        
        //now flush the commits here
        mutator.flush();
        
        addActionMessage("Put (batching) is successful");
        
        return SUCCESS;
    }
    
    public void testPut(BufferedMutator mutator) throws IOException {
        for (int i = 0 ; i < 30; i ++) {
            Put put = new Put(("small-" + i).getBytes());
            put.add(CF_CF, COLUMN_A, String.valueOf(i).getBytes());
            mutator.mutate(put);
        }
        
        method(mutator.getName() != null ? mutator.getName().getName() : null);
    }
    
    private void method(byte[] name) {}
}
 