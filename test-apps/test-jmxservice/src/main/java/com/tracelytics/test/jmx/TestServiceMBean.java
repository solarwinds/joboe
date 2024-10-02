package com.tracelytics.test.jmx;

import org.jboss.system.ServiceMBean;

public interface TestServiceMBean extends ServiceMBean
{
    // Configure getters and setters for the message attribute
    String getMessage();
    void setMessage(String message);

    // The print message operation
    void printMessage();
    
    String testOp(Integer a);
}
