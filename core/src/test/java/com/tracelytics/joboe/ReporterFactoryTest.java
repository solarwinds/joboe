package com.tracelytics.joboe;

import junit.framework.TestCase;

import java.lang.reflect.Field;
import java.net.InetAddress;


public class ReporterFactoryTest extends TestCase {
    public void testbuildDefaultUdpReporter() throws Exception {
        UDPReporter reporter = (UDPReporter) ReporterFactory.getInstance().buildUdpReporter();
        
        Field addressField = reporter.getClass().getDeclaredField("addr");
        addressField.setAccessible(true);
        
        Field portField = reporter.getClass().getDeclaredField("port");
        portField.setAccessible(true);
        
        InetAddress address = (InetAddress) addressField.get(reporter);
        assertEquals(InetAddress.getByName(Constants.XTR_UDP_HOST), address);
        assertEquals(Constants.XTR_UDP_PORT, portField.get(reporter));
    }
    
    
    public void testbuildNonDefaultUdpReporter() throws Exception {
        UDPReporter reporter = (UDPReporter) ReporterFactory.getInstance().buildUdpReporter("localhost", 9999);
        
        Field addressField = reporter.getClass().getDeclaredField("addr");
        addressField.setAccessible(true);
        
        Field portField = reporter.getClass().getDeclaredField("port");
        portField.setAccessible(true);
        
        InetAddress address = (InetAddress) addressField.get(reporter);
        assertEquals(InetAddress.getByName("localhost"), address);
        assertEquals(9999, portField.get(reporter));
    }
}
