package com.tracelytics.test.action;

import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.client.Increment;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.RowMutations;
import org.apache.struts2.convention.annotation.Results;


@Results({
    @org.apache.struts2.convention.annotation.Result(name="success", location="index.jsp"),
    @org.apache.struts2.convention.annotation.Result(name="error", location="index.jsp"),
    @org.apache.struts2.convention.annotation.Result(name="input", location="index.jsp")
})
public class TestMutateRow extends AbstractHBaseTest {
    private static final byte[] TEMP = "temp".getBytes();
    public String execute() throws Exception {
        Table table = getTable(TABLE_TEST);
        
        RowMutations mutations = new RowMutations(ROW_TEST);
        mutations.add(new Put(ROW_TEST).add(CF_CF, TEMP, TEMP));
        mutations.add(new Delete(ROW_TEST).deleteColumn(CF_CF, TEMP));
        table.mutateRow(mutations);
        
        addActionMessage("MutateRow is successful");
        
        return SUCCESS;
    }
}
