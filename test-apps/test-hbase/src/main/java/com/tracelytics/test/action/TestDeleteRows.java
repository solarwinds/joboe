package com.tracelytics.test.action;

import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.BufferedMutator;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.Table;
import org.apache.struts2.convention.annotation.Results;


@Results({
    @org.apache.struts2.convention.annotation.Result(name="success", location="index.jsp"),
    @org.apache.struts2.convention.annotation.Result(name="error", location="index.jsp"),
    @org.apache.struts2.convention.annotation.Result(name="input", location="index.jsp")
})
public class TestDeleteRows extends AbstractHBaseTest {
    private int count;
    
    
    
    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public String execute() throws Exception {
        BufferedMutator mutator = getMutator(TABLE_TEST);
        
        List<Delete> deletes = new ArrayList<Delete>();
        for (int i = 0 ; i < count; i ++) {
            deletes.add(new Delete(("small-" + i).getBytes()));
        }
        mutator.mutate(deletes);        
        
        mutator.flush();
        
        addActionMessage("Deleted [" + count + "] row(s)");
        return SUCCESS;
    }
    
}
