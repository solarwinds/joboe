package com.tracelytics.test.action;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.coprocessor.Batch;
import org.apache.hadoop.hbase.ipc.BlockingRpcCallback;
import org.apache.hadoop.hbase.protobuf.generated.AggregateProtos.AggregateRequest;
import org.apache.hadoop.hbase.protobuf.generated.AggregateProtos.AggregateResponse;
import org.apache.hadoop.hbase.protobuf.generated.AggregateProtos.AggregateService;
import org.apache.hadoop.hbase.protobuf.generated.MultiRowMutationProtos.MultiRowMutationService;
import org.apache.hadoop.hbase.protobuf.generated.MultiRowMutationProtos.MutateRowsRequest;
import org.apache.hadoop.hbase.protobuf.generated.MultiRowMutationProtos.MutateRowsResponse;
import org.apache.hadoop.hbase.protobuf.generated.RowProcessorProtos.ProcessRequest;
import org.apache.hadoop.hbase.protobuf.generated.RowProcessorProtos.ProcessResponse;
import org.apache.hadoop.hbase.protobuf.generated.RowProcessorProtos.RowProcessorService;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.struts2.convention.annotation.Results;

import com.google.protobuf.Descriptors.MethodDescriptor;
import com.google.protobuf.Descriptors.ServiceDescriptor;
import com.google.protobuf.Message;
import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcController;
import com.google.protobuf.Service;


@Results({
    @org.apache.struts2.convention.annotation.Result(name="success", location="index.jsp"),
    @org.apache.struts2.convention.annotation.Result(name="error", location="index.jsp"),
    @org.apache.struts2.convention.annotation.Result(name="input", location="index.jsp")
})
public class TestCoprocessorService extends AbstractHBaseTest {
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
            
            Map<byte[], Long> results = table.coprocessorService(MultiRowMutationService.class, null, null, new Batch.Call<MultiRowMutationService, Long>() {
                public Long call(MultiRowMutationService instance)
                    throws IOException {
                    BlockingRpcCallback<MutateRowsResponse> rpcCallback = new BlockingRpcCallback<MutateRowsResponse>();
                    instance.mutateRows(null, MutateRowsRequest.getDefaultInstance(), rpcCallback);
                    return 1l;
                }
                
            });
        } catch (Throwable e) {
            e.printStackTrace();
        }
        
        addActionMessage("coprocessorService is successful");
        
        return SUCCESS;
    }

    private byte[] generateLongValueByteArray() {
        byte[] array = new byte[Bytes.SIZEOF_LONG];
        random.nextBytes(array); 
        
        return array;
    }
    
    private class MyService implements Service{

        public ServiceDescriptor getDescriptorForType() {
            // TODO Auto-generated method stub
            return null;
        }

        public void callMethod(MethodDescriptor method, RpcController controller, Message request, RpcCallback<Message> done) {
            // TODO Auto-generated method stub
            
        }

        public Message getRequestPrototype(MethodDescriptor method) {
            // TODO Auto-generated method stub
            return null;
        }

        public Message getResponsePrototype(MethodDescriptor method) {
            // TODO Auto-generated method stub
            return null;
        }
        
    }
}
