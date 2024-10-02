package com.tracelytics.test.action;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.filter.BinaryComparator;
import org.apache.hadoop.hbase.filter.CompareFilter.CompareOp;
import org.apache.hadoop.hbase.filter.ValueFilter;
import org.apache.struts2.convention.annotation.Results;

import com.opensymphony.xwork2.ActionSupport;


@Results({
    @org.apache.struts2.convention.annotation.Result(name="success", location="index.jsp"),
    @org.apache.struts2.convention.annotation.Result(name="error", location="index.jsp"),
    @org.apache.struts2.convention.annotation.Result(name="input", location="index.jsp")
})
public class TestScanner extends AbstractHBaseTest {
    
    public String execute() throws Exception {
        
        
        HTable table = getTable(TABLE_TEST);
        
        testScan(table);
        
        addActionMessage("Scan is successful");
        
        return SUCCESS;
    }
    
      
  
    private void testScan(HTable table) throws IOException {
        Scan scan  = new Scan();
        scan.setCaching(10000);
        
        ResultScanner scanner = null;

        System.out.println("Scanner with family and filter");
        scan.addColumn(CF_CF, COLUMN_A);
        
        try { 
            scanner = table.getScanner(scan);
            printScannerResult(scanner);
        } finally {
            scanner.close();
        }
    }
    
    private static void printScannerResult(ResultScanner scanner) throws IOException {
        for (Result entry : scanner) {
            //System.out.println(entry.toString());//just loop through it
        }
    }
    
}
