package com.tracelytics.test;

import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

public class WebSessionListener implements HttpSessionListener {
    @Override
    public void sessionDestroyed(HttpSessionEvent sessionEvent) {
        WebsocketOutputServer.removeOutputServer(sessionEvent.getSession().getId());
    }

    @Override
    public void sessionCreated(HttpSessionEvent se) {
        // TODO Auto-generated method stub
        
    }
}
