package com.tracelytics.instrumentation;

import javax.servlet.Servlet;

import com.tracelytics.instrumentation.http.HttpServletStub;
import com.tracelytics.instrumentation.http.ServletInstrumentation;

/**
 * Test to make sure even exception is thrown by our instrumentation code, the exception would be caught within CtBehavior and would not
 * bubble all the up to the caller
 * 
 * @author Patson Luk
 *
 */
public class ExceptionHandlingInstrumentationTest extends AbstractInstrumentationTest<ServletInstrumentation> {
    public void testException() throws Exception {
        Servlet servlet = new HttpServletStub();        
        
        servlet.service(null, null); //deliberately trigger NPE from the instrumentation and make sure the code in CtBehavior catches it correcly
        
        System.out.println("previous exception output is expected. But it should run to here without interruption to the flow");
    }
}