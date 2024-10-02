package com.tracelytics.instrumentation.http;

import java.net.URL;
import java.net.URLConnection;

import com.tracelytics.instrumentation.AbstractInstrumentationTest;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.Metadata;


public class UrlConnectorPatcherTest extends AbstractInstrumentationTest<HttpURLConnectionInstrumentation> {
    

    
    public void testHandle() throws Exception {
        try {
            URLConnection connection = new URL("http://www.google.com").openConnection();
            
            assertNull(connection.getRequestProperty("X-Trace"));
            
            connection.connect();
            
            assertNotNull(connection.getRequestProperty("X-Trace"));
            
            assertEquals(Context.getMetadata().taskHexString(), new Metadata(connection.getRequestProperty("X-Trace")).taskHexString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    
}