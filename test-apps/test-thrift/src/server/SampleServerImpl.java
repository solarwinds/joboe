package server;

import java.util.*;
import org.apache.thrift.*;
import sample.*;

class SampleServerImpl implements Sample.Iface
{
  public void ping() throws TException
  {
    long time = System.currentTimeMillis();
    System.out.println("Ping: "+time);
  }

  public boolean sendMessage(Message msg) throws TException
  {
    long time = System.currentTimeMillis();
    System.out.println("sendMessage: msgID="+msg.getMsgID() + " text=" + msg.getText());
    return true;
  }

}
