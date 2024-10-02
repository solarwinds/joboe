package com.tracelytics.test.action;

import java.util.regex.Pattern;

import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.struts2.convention.annotation.Results;


@Results({
    @org.apache.struts2.convention.annotation.Result(name="success", location="index.jsp"),
    @org.apache.struts2.convention.annotation.Result(name="error", location="index.jsp"),
    @org.apache.struts2.convention.annotation.Result(name="input", location="index.jsp")
})
public class TestTable extends AbstractHBaseTest {
    private static final TableName TABLE_TEMP = TableName.valueOf("tempTable");
    
    public String execute() throws Exception {
        Admin admin = ConnectionFactory.createConnection(DEFAULT_CONFIG).getAdmin();
        
        HTableDescriptor tableDescriptor = new HTableDescriptor(TABLE_TEMP);
        tableDescriptor.addFamily(new HColumnDescriptor("cf"));
        admin.createTable(tableDescriptor);
        admin.disableTable(TABLE_TEMP);
        admin.modifyTable(TABLE_TEMP, tableDescriptor);
        admin.enableTable(TABLE_TEMP);
        admin.disableTable(TABLE_TEMP);
        admin.deleteTable(TABLE_TEMP);
        
        admin.createTableAsync(tableDescriptor, null);
        
        int counter;
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
