package com.tracelytics.test.thrift.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;

import com.appoptics.api.ext.Trace;
import com.tracelytics.test.thrift.ServiceExample;

public class ThriftPlainClient {
    public static void main(String[] args)
        throws UnknownHostException, IOException {
        Trace.startTrace("my-thrift").report();

        TTransport transport = null;
        try {
            transport = new TSocket("localhost", 8081);
            transport.open();

            TProtocol protocol = new TBinaryProtocol(transport);
            ServiceExample.Client client = new ServiceExample.Client(protocol);

            System.out.println(client.getBean(1, "string"));
            
            client.testOneWay();
        } catch (TTransportException e) {
            e.printStackTrace();
        } catch (TException e) {
            e.printStackTrace();
        } finally {
            if (transport != null) {
                transport.close();
            }
        }

        try {
            transport = new TSocket(new Socket("localhost", 8081));

            TProtocol protocol = new TBinaryProtocol(transport);
            ServiceExample.Client client = new ServiceExample.Client(protocol);

            System.out.println(client.getBean(1, "string"));
        } catch (TTransportException e) {
            e.printStackTrace();
        } catch (TException e) {
            e.printStackTrace();
        } finally {
            if (transport != null) {
                transport.close();
            }
        }

        Trace.endTrace("my-thrift");
    }
}
