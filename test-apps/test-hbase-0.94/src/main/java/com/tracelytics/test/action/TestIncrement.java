package com.tracelytics.test.action;

import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Increment;
import org.apache.struts2.convention.annotation.Results;


@Results({
    @org.apache.struts2.convention.annotation.Result(name="success", location="index.jsp"),
    @org.apache.struts2.convention.annotation.Result(name="error", location="index.jsp"),
    @org.apache.struts2.convention.annotation.Result(name="input", location="index.jsp")
})
public class TestIncrement extends AbstractHBaseTest {
    private static final byte[] Q_LONG_VALUE = "long-value".getBytes();
    public String execute() throws Exception {
        HTable table = getTable(TABLE_TEST);
        
        Increment increment = new Increment(ROW_TEST);
        increment.addColumn(CF_CF, Q_LONG_VALUE, 5);
        
        table.increment(increment);
        table.incrementColumnValue(ROW_TEST, CF_CF, Q_LONG_VALUE, -5);
        
        addActionMessage("Increment is successful");
        
        return SUCCESS;
    }
}
