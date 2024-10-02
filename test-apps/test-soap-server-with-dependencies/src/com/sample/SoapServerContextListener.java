package com.sample;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class SoapServerContextListener implements ServletContextListener {
//    private Thread soapServerThread;
    public void contextInitialized(ServletContextEvent sce) {
        WsPublisher.main(new String[0]);
    }

    public void contextDestroyed(ServletContextEvent sce) {
//        if (soapServerThread != null) {
//            soapServerThread.stop();
//        }
    }

}
