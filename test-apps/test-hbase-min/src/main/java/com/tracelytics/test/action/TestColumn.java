package com.tracelytics.test.action;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.client.HBaseAdmin;
import org.apache.struts2.convention.annotation.Results;


@Results({
    @org.apache.struts2.convention.annotation.Result(name="success", location="index.jsp"),
    @org.apache.struts2.convention.annotation.Result(name="error", location="index.jsp"),
    @org.apache.struts2.convention.annotation.Result(name="input", location="index.jsp")
})
public class TestColumn extends AbstractHBaseTest {
    private static final byte[] CF_TEMP = "temp-cf".getBytes();
    
    public String execute() throws Exception {
        
        HBaseAdmin admin = new HBaseAdmin(DEFAULT_CONFIG);
        
        admin.disableTable(TABLE_TEST.getBytes());
        
        admin.addColumn(TABLE_TEST.getBytes(), new HColumnDescriptor(CF_TEMP));
        
        admin.deleteColumn(TABLE_TEST.getBytes(), CF_TEMP);
        
        admin.enableTableAsync(TABLE_TEST.getBytes());
        int counter = 0;
        while (!admin.isTableEnabled(TABLE_TEST) && counter++ < 10) { //wait 10 times 
            Thread.sleep(1000);
        }
        
        addActionMessage("Column operations are successful");
        
        return SUCCESS;
    }
}
