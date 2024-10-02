package com.sample;

import javax.xml.ws.Endpoint;

public class WsPublisher {
    public static void main(String[] args) {
        Endpoint.publish("http://0.0.0.0:8888/ws/server", new Operator());
        
        System.out.println("SOAP server started locally at port 8888");
    }
}
