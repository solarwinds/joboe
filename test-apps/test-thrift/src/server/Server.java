package server;

import java.io.*;
import org.apache.thrift.protocol.*;
import org.apache.thrift.protocol.TBinaryProtocol.*;
import org.apache.thrift.server.*;
import org.apache.thrift.transport.*;
import sample.*;

public class Server
{
  private void start()
  {
    try
    {
      TServerSocket serverTransport = new TServerSocket(7911);
      Sample.Processor processor = new Sample.Processor(new SampleServerImpl());
      TServer server = new TThreadPoolServer(new TThreadPoolServer.Args(serverTransport).processor(processor));

      System.out.println("Starting server on port 7911 ...");
      server.serve();
    }catch(TTransportException e)
    {
      e.printStackTrace();
    }
  }

  public static void main(String[] args)
  {
    Server srv = new Server();
    srv.start();
  }
}
