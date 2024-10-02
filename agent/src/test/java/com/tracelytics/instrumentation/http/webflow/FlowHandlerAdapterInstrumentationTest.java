package com.tracelytics.instrumentation.http.webflow;

import java.util.ArrayList;
import java.util.List;

import org.springframework.webflow.mvc.servlet.AbstractFlowHandler;
import org.springframework.webflow.mvc.servlet.FlowHandlerAdapter;

import com.tracelytics.ExpectedEvent;
import com.tracelytics.instrumentation.AbstractInstrumentationTest;
import com.tracelytics.instrumentation.http.HttpServletRequestStub;
import com.tracelytics.instrumentation.http.HttpServletResponseStub;


public class FlowHandlerAdapterInstrumentationTest extends AbstractInstrumentationTest<FlowHandlerAdapterInstrumentation> {
    

    
    public void testHandle() throws Exception {
        FlowHandlerAdapter flowHandlerAdapter = new MockFlowHandlerAdapter();        
        
        flowHandlerAdapter.handle(new HttpServletRequestStub(), 
                              new HttpServletResponseStub(), 
                              new AbstractFlowHandler(){
            
        });
        
        List<ExpectedEvent> expectedEvents = new ArrayList<ExpectedEvent>();
        
        ExpectedEvent event;
        
        event = new ExpectedEvent();
        event.addInfo("Layer", "webflow");
        event.addInfo("Label", "entry");
        expectedEvents.add(event);
        
        event = new ExpectedEvent();
        event.addInfo("Layer", "webflow");
        event.addInfo("Label", "exit");
        expectedEvents.add(event);
        
        assertEvents(expectedEvents);
    }
    
    
}