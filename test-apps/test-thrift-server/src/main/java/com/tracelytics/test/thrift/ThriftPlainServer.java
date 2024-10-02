package com.tracelytics.test.thrift;

import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TSimpleServer;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TServerTransport;
import org.apache.thrift.transport.TTransportException;

import com.tracelytics.test.thrift.ServiceExample.Iface;
import com.tracelytics.test.thrift.ServiceExample.Processor;

/**
 * Starts Thrift sample server as a standalone
 * @author Patson Luk
 *
 */
public class ThriftPlainServer {

    public static void main(String[] args) throws TTransportException {
        TServerTransport serverTransport = new TServerSocket(8081);
        //             TNonblockingServerTransport serverTransport = new TNonblockingServerSocket(8081);
        //            TNonblockingServerTransport serverTransport = new TCustomNonblockingServerSocket(new InetSocketAddress(8081), true, 409600, 409600);

        Processor<Iface> processor = new ServiceExample.Processor<ServiceExample.Iface>(new ServiceExampleImpl());

        final TServer server = new TSimpleServer(new TServer.Args(serverTransport).processor(processor));
        //final TServer server = new TThreadPoolServer(new TThreadPoolServer.Args(serverTransport).processor(processor).inputProtocolFactory(new TCompactProtocol.Factory()).outputProtocolFactory(new TCompactProtocol.Factory()));
        //             final TServer server = new TNonblockingServer(new TNonblockingServer.Args(serverTransport).processor(processor).inputProtocolFactory(new TCompactProtocol.Factory()).outputProtocolFactory(new TCompactProtocol.Factory()));

        //            final TServer server = new THsHaServer(new THsHaServer.Args(serverTransport).processor(processor).inputProtocolFactory(new TCompactProtocol.Factory()).outputProtocolFactory(new TCompactProtocol.Factory()));
        //          

        System.out.println("Starting server on port 8081 ...");

        server.serve();

    }
}
