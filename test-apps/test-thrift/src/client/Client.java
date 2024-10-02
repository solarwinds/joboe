package client;

import java.net.*;
import org.apache.thrift.*;
import org.apache.thrift.protocol.*;
import org.apache.thrift.transport.*;

import com.tracelytics.api.*;
import sample.*;

public class Client {
  private void start(int numReqs) {
    TTransport transport;
    try {
      transport = new TSocket("localhost", 7911);
      TProtocol protocol = new TBinaryProtocol(transport);
      // this client depends on the client class in the gen-java/tserver/gen/TimeServer.java file
      // public static class Client implements TServiceClient, Iface
      Sample.Client client = new Sample.Client(protocol); 
      transport.open();
      for (int i=0; i<numReqs; i++) {
        client.ping();
      }
      System.out.println(client.sendMessage(new Message(1,"test")));
      transport.close();
    } catch (TTransportException e) {
      e.printStackTrace();
    } catch (TException e) {
      e.printStackTrace();
    }
  }

  public static void main(String[] args) {
    TraceEvent event = Trace.startTrace("TestThrift");
    event.addInfo("URL","/thrift/test");
    event.report();

    int numReqs = 1;
    if (args.length > 0) {
        numReqs = Integer.parseInt(args[0]);
    }
        
    Client c = new Client();
    c.start(numReqs);

    String xTrace = Trace.endTrace("TestThrift");
    System.out.println("End trace: " + xTrace);
  }
}
