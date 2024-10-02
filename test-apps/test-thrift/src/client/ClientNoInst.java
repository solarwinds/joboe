package client;

import java.net.*;
import org.apache.thrift.*;
import org.apache.thrift.protocol.*;
import org.apache.thrift.transport.*;

import sample.*;

public class ClientNoInst {
  private void start(){
    TTransport transport;
    try {
      transport = new TSocket("localhost", 7911);
      TProtocol protocol = new TBinaryProtocol(transport);
      // this client depends on the client class in the gen-java/tserver/gen/TimeServer.java file
      // public static class Client implements TServiceClient, Iface
      Sample.Client client = new Sample.Client(protocol); 
      transport.open();
      client.ping();
      System.out.println(client.sendMessage(new Message(1,"test")));
      transport.close();
    } catch (TTransportException e) {
      e.printStackTrace();
    } catch (TException e) {
      e.printStackTrace();
    }
  }

  public static void main(String[] args) {
    ClientNoInst c = new ClientNoInst();
    c.start();
  }
}
