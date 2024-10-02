package com.tracelytics.test.action;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.BufferedMutator;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.client.Put;
import org.apache.struts2.convention.annotation.Results;

import com.opensymphony.xwork2.ActionSupport;


@Results({
    @org.apache.struts2.convention.annotation.Result(name="success", location="index.jsp"),
    @org.apache.struts2.convention.annotation.Result(name="error", location="index.jsp"),
    @org.apache.struts2.convention.annotation.Result(name="input", location="index.jsp")
})
public class TestBigDelete extends AbstractHBaseTest {
    
    public String execute() throws Exception {
        BufferedMutator mutator = getMutator(TABLE_TEST);
        
        List<Delete> deletes = new ArrayList<Delete>();
        
        for (int i = 0 ; i < 30; i ++) {
            deletes.add(new Delete(("big-" + (i + 30)).getBytes()));
        }
        
        mutator.mutate(deletes);
        
        mutator.flush();
        
        addActionMessage("Delete is successful");
        
        return SUCCESS;
    }
}
