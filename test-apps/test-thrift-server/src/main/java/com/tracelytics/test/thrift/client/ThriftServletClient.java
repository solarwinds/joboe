package com.tracelytics.test.thrift.client;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.THttpClient;
import org.apache.thrift.transport.TTransportException;

import com.tracelytics.test.thrift.BeanExample;
import com.tracelytics.test.thrift.ServiceExample;

public class ThriftServletClient {
    public static void main(String[] args) {
        String servletUrl = "http://localhost:8080/test-thrift-server/ThriftServlet";
        
        try {
            THttpClient httpClient = new THttpClient(servletUrl);
            TProtocol thriftProtocol = new TCompactProtocol(httpClient);
            
            ServiceExample.Client client = new ServiceExample.Client(thriftProtocol);
            
            BeanExample bean = client.getBean(1, "string");
            
            System.out.println(bean.getStringObject());
        } catch (TTransportException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (TException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        
        
    }
}
