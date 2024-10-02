package com.tracelytics.test.action;

import java.io.IOException;

import org.apache.struts2.ServletActionContext;

import com.tracelytics.test.WebsocketOutputServer;

/**
 * Convenient class for writing to web-socket outputs when there's struts context available
 * @author pluk
 *
 */
class WebsocketWriter {
    private WebsocketWriter() {}
    
    static void write(String message) throws IOException {
        write(null, message);
    }
    
    static void write(String socketId, String message) throws IOException {
        String sessionId = ServletActionContext.getRequest().getSession().getId();
        WebsocketOutputServer.outputMessage(sessionId, socketId, message);
        
    }
}
