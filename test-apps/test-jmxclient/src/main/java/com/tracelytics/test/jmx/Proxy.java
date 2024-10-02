// From http://www.hsc.fr/ressources/outils/jisandwis/download/
package com.tracelytics.test.jmx;

import javax.management.MBeanServerConnection;
import org.jboss.console.remote.RemoteMBeanAttributeInvocation;
import org.jboss.console.remote.RemoteMBeanInvocation;


public class Proxy {

  MBeanServerConnection mbeanServer;

  public Proxy(MBeanServerConnection mbeanServer) {
    this.mbeanServer = mbeanServer;
  }

  public String invoke(RemoteMBeanInvocation invocation) throws Exception {
    String resultToString = "";
    Object result = mbeanServer.invoke(invocation.targetObjectName, invocation.actionName, invocation.params, invocation.signature);
    if(result != null) {
      resultToString = TypeConvertor.convertObjectToString(result, getObjectClass(result)) + "\n";
    }
    return resultToString;
  }

  public String get(RemoteMBeanAttributeInvocation invocation) throws Exception {
    String resultToString = "";
    Object result = mbeanServer.getAttribute(invocation.targetObjectName, invocation.attributeName);
    if(result != null) {
      resultToString = TypeConvertor.convertObjectToString(result, getObjectClass(result)) + "\n";
    }
    return resultToString;
  }

  private String getObjectClass(Object object) {
    return object.getClass().toString().split(" ")[1];
  }
}
