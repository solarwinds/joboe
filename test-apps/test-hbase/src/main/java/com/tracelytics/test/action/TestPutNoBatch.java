package com.tracelytics.test.action;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.client.Put;
import org.apache.struts2.convention.annotation.Results;

import com.opensymphony.xwork2.ActionSupport;

@Results({
          @org.apache.struts2.convention.annotation.Result(name = "success", location = "index.jsp"),
          @org.apache.struts2.convention.annotation.Result(name = "error", location = "index.jsp"),
          @org.apache.struts2.convention.annotation.Result(name = "input", location = "index.jsp")
})
public class TestPutNoBatch extends AbstractHBaseTest {

    public String execute() throws Exception {
        
        Table table = getTable(TABLE_TEST);

        testPut(table);

        addActionMessage("Put (no batching) is successful");

        return SUCCESS;
    }

    public void testPut(Table table) throws IOException {
        for (int i = 0; i < 30; i++) {
            Put put = new Put(("small-" + i).getBytes());
            put.add(CF_CF, COLUMN_A, String.valueOf(i).getBytes());
            table.put(put);
        }
    }
}
