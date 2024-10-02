package com.tracelytics.test.action;

import org.apache.hadoop.hbase.client.HTable;
import org.apache.struts2.convention.annotation.Results;


@Results({
    @org.apache.struts2.convention.annotation.Result(name="success", location="index.jsp"),
    @org.apache.struts2.convention.annotation.Result(name="error", location="index.jsp"),
    @org.apache.struts2.convention.annotation.Result(name="input", location="index.jsp")
})
public class TestGetRowOrBefore extends AbstractHBaseTest {
    public String execute() throws Exception {
        HTable table = getTable(TABLE_TEST);
        
        table.getRowOrBefore(ROW_TEST, CF_CF);
        
        addActionMessage("GetRowOrBefore is successful");
        
        return SUCCESS;
    }
}
