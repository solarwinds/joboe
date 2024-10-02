package com.tracelytics.test.action;

import java.util.regex.Pattern;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.client.HBaseAdmin;
import org.apache.struts2.convention.annotation.Results;

import com.opensymphony.xwork2.ActionSupport;


@Results({
    @org.apache.struts2.convention.annotation.Result(name="success", location="index.jsp"),
    @org.apache.struts2.convention.annotation.Result(name="error", location="index.jsp"),
    @org.apache.struts2.convention.annotation.Result(name="input", location="index.jsp")
})
public class TestTable extends AbstractHBaseTest {
    private static final byte[] TABLE_TEMP = "tempTable".getBytes();
    
    public String execute() throws Exception {
        HBaseAdmin admin = new HBaseAdmin(DEFAULT_CONFIG);
        
        HTableDescriptor tableDescriptor = new HTableDescriptor("tempTable");
        
        admin.createTable(tableDescriptor);
        admin.disableTable(TABLE_TEMP);
        admin.modifyTable(TABLE_TEMP, tableDescriptor);
        admin.enableTableAsync(TABLE_TEMP);
        
        int counter;
        
        counter = 0;
        while (!admin.isTableEnabled(TABLE_TEMP) && counter++ < 10) { //wait 10 times 
            Thread.sleep(1000);
        }
        
        admin.disableTable(TABLE_TEMP);
        admin.deleteTable(TABLE_TEMP);
        
        admin.createTableAsync(tableDescriptor, null);
        counter = 0;
        while (!admin.isTableAvailable(TABLE_TEMP) && counter++ < 10) { //wait 10 times 
            Thread.sleep(1000);
        }
        
        admin.disableTableAsync(TABLE_TEMP);
        counter = 0;
        while (!admin.isTableDisabled(TABLE_TEMP) && counter++ < 10) { //wait 10 times 
            Thread.sleep(1000);
        }
        admin.enableTableAsync(TABLE_TEMP);
        
        counter = 0;
        while (!admin.isTableEnabled(TABLE_TEMP) && counter++ < 10) { //wait 10 times 
            Thread.sleep(1000);
        }
        admin.disableTable(TABLE_TEMP);
        
        admin.deleteTable(TABLE_TEMP);
        
        admin.deleteTables("notExisted");
        admin.deleteTables(Pattern.compile("notExisted"));
        
        addActionMessage("Table operations are successful");
        
        return SUCCESS;
    }
}
