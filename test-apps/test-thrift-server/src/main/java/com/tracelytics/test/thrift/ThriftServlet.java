package com.tracelytics.test.thrift;

import javax.servlet.Servlet;

import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.server.TServlet;

/**
 * Servlet implementation class ThriftServlet
 */
public class ThriftServlet extends TServlet implements Servlet {
	private static final long serialVersionUID = 1L;

	public ThriftServlet() {
        super(new ServiceExample.Processor<ServiceExample.Iface>(new ServiceExampleImpl()), new TCompactProtocol.Factory());
    }
   
  
}
