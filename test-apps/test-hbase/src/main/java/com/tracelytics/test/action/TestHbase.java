package com.tracelytics.test.action;

import java.io.IOException;
import java.util.Arrays;

import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.filter.BinaryComparator;
import org.apache.hadoop.hbase.filter.CompareFilter.CompareOp;
import org.apache.hadoop.hbase.filter.ValueFilter;
import org.apache.struts2.convention.annotation.Results;


@Results({
    @org.apache.struts2.convention.annotation.Result(name="success", location="index.jsp"),
    @org.apache.struts2.convention.annotation.Result(name="error", location="index.jsp"),
    @org.apache.struts2.convention.annotation.Result(name="input", location="index.jsp")
})
public class TestHbase extends AbstractHBaseTest {
    
    public String execute() throws Exception {
        Table table = getTable(TABLE_TEST);
        
        
        System.out.println("Testing PUT");
        testPut(table);
        
        System.out.println("Testing GET");
        testGet(table);

        return SUCCESS;
    }
    
   

    public void testPut(Table table) throws IOException {
        Put put = new Put(ROW_TEST);
        put.add(CF_CF, COLUMN_A, "value4".getBytes());
        put.add(CF_BLOB, COLUMN_DATA, "value5".getBytes());
        table.put(put);
        
        table.put(Arrays.asList(put, put, put));
    }
    
    private void testGet(Table table) throws IOException {
        Result result = table.get(new Get(ROW_TEST));
        result.getRow();
        
        table.get(Arrays.asList(new Get(ROW_TEST), new Get(ROW_TEST), new Get(ROW_TEST)));

        result = table.get(new Get(ROW_TEST));
        result.raw();
    }
    
    private void testScan(Table table) throws IOException {
        System.out.println("Empty scanner");
        Scan scan  = new Scan();
        scan.setCaching(1000);
        
        ResultScanner scanner = null; 
               
        try { 
            scanner = table.getScanner(scan);
            printScannerResult(scanner);
        } finally {
            scanner.close();
        }

        System.out.println("Scanner with family and filter");
        scan.addFamily("cf".getBytes());
        scan.setFilter(new ValueFilter(CompareOp.NOT_EQUAL, new BinaryComparator("nono".getBytes())));
        
        try { 
            scanner = table.getScanner(scan);
            printScannerResult(scanner);
        } finally {
            scanner.close();
        }
    }
    
    private static void printScannerResult(ResultScanner scanner) throws IOException {
        Result result;
        while ((result = scanner.next()) != null) {
            byte[] value = result.getValue(CF_CF, COLUMN_A);
            System.out.println(value != null ? new String(value) : null);
        }
    }
   
    
    
}
