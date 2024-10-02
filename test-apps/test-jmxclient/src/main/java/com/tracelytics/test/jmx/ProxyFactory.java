// From http://www.hsc.fr/ressources/outils/jisandwis/download/
package com.tracelytics.test.jmx;

import javax.management.ObjectName;
import org.jboss.invocation.http.interfaces.HttpInvokerProxy;
import org.jboss.invocation.Invoker;
import org.jboss.jmx.adaptor.rmi.RMIAdaptorExt;
import org.jboss.proxy.GenericProxyFactory;
import javax.management.MBeanServerConnection;
import java.util.ArrayList;

import org.jboss.console.remote.RemoteMBeanAttributeInvocation;
import org.jboss.console.remote.RemoteMBeanInvocation;


public class ProxyFactory {

  public static Proxy createProxy(String url) throws Exception {

    Object cacheID = null;
    ObjectName targetName = new ObjectName("jboss.jmx:type=adaptor,name=Invoker");
    Invoker invoker = new HttpInvokerProxy(url);
    String jndiName = null;
    String proxyBindingName = null;

    /* Building the interceptors list */
    /* These interceptors will be executed from the client side */
    ArrayList interceptorClasses = new ArrayList();
    interceptorClasses.add(Class.forName("org.jboss.proxy.ClientMethodInterceptor"));
    interceptorClasses.add(Class.forName("org.jboss.proxy.SecurityInterceptor"));
    interceptorClasses.add(Class.forName("org.jboss.jmx.connector.invoker.client.InvokerAdaptorClientInterceptor"));
    interceptorClasses.add(Class.forName("org.jboss.invocation.InvokerInterceptor"));

    /* Getting the current classloader */
    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

    /* Building the exported interfaces list */
    /* The final proxy will implement these interfaces */
    Class[] interfaces = new Class[] {
      Class.forName("org.jboss.jmx.adaptor.rmi.RMIAdaptorExt")
    };

    GenericProxyFactory proxyFactory = new GenericProxyFactory();
    MBeanServerConnection server = (MBeanServerConnection) proxyFactory.createProxy(
      cacheID,
      targetName,
      invoker,
      jndiName,
      proxyBindingName,
      interceptorClasses,
      classLoader,
      interfaces
    );

    return new Proxy(server);
  }

}
