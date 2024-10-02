package com.tracelytics.test.action;

import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Row;
import org.apache.hadoop.hbase.client.Table;
import org.apache.struts2.convention.annotation.Results;


@Results({
    @org.apache.struts2.convention.annotation.Result(name="success", location="index.jsp"),
    @org.apache.struts2.convention.annotation.Result(name="error", location="index.jsp"),
    @org.apache.struts2.convention.annotation.Result(name="input", location="index.jsp")
})
public class TestBatch extends AbstractHBaseTest {
    private static final byte[] ROW_TEMP = "temp-row".getBytes();
    
    public String execute() throws Exception {
        
        Table table = getTable(TABLE_TEST);
        
        List<Row> actions = new ArrayList<Row>();
        actions.add(new Put(ROW_TEMP).add(CF_CF, COLUMN_A, "temp".getBytes()));
        actions.add(new Get(ROW_TEMP));
        actions.add(new Delete(ROW_TEMP));
        
        table.batch(actions);
        table.batch(actions, new Object[actions.size()]);
        
        addActionMessage("Batch is successful");
        
        return SUCCESS;
    }
}
