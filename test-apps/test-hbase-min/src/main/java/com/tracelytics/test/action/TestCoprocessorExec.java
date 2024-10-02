package com.tracelytics.test.action;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.client.coprocessor.Batch;
import org.apache.hadoop.hbase.client.coprocessor.LongColumnInterpreter;
import org.apache.hadoop.hbase.coprocessor.AggregateProtocol;
import org.apache.hadoop.hbase.ipc.HBaseRPC.UnknownProtocolException;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.struts2.convention.annotation.Results;


@Results({
    @org.apache.struts2.convention.annotation.Result(name="success", location="index.jsp"),
    @org.apache.struts2.convention.annotation.Result(name="error", location="index.jsp"),
    @org.apache.struts2.convention.annotation.Result(name="input", location="index.jsp")
})
public class TestCoprocessorExec extends AbstractHBaseTest {
    private static final byte[] Q_LONG_VALUE = "long-value".getBytes();
    private static Random random = new Random();
    
    public String execute() throws Exception {
        
        HTable table = getTable(TABLE_TEST);
        
        //commented out code used to populate the table
//        ResultScanner scanner = table.getScanner(CF_CF);
//        
//        List<Put> puts = new ArrayList<Put>();
//        Result result;
//        while ((result = scanner.next()) != null) {
//            if (result.getValue(CF_CF, Q_LONG_VALUE) == null) {
//                puts.add(new Put(result.getRow()).add(CF_CF, Q_LONG_VALUE, generateLongValueByteArray()));
//            }
//        }
//        
//        table.put(puts);
        
        try {
            Map<byte[], Long> results = table.coprocessorExec(AggregateProtocol.class, null, null, new Batch.Call<AggregateProtocol, Long>() {
                public Long call(AggregateProtocol instance)
                    throws IOException {
                    try {
                        return instance.getSum(new LongColumnInterpreter(), new Scan().addColumn(CF_CF, Q_LONG_VALUE));
                    } catch (UnknownProtocolException e) {
                        e.printStackTrace();
                        return null;
                    }
                }
                
            });
            
            for (Entry<byte[], Long> entry : results.entrySet()) {
                System.out.println(new String(entry.getKey()) + " : " + entry.getValue());
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        
        addActionMessage("coprocessorExec is successful");
        
        return SUCCESS;
    }

    private byte[] generateLongValueByteArray() {
        byte[] array = new byte[Bytes.SIZEOF_LONG];
        random.nextBytes(array); 
        
        return array;
    }
}
