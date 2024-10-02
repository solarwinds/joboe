package com.appoptics.test.ws;

import java.rmi.RemoteException;

import javax.xml.rpc.ServiceException;

public class TestDriver {
    private static final String ENDPOINT = "http://localhost:8080/test-jax-ws-server/hello";
//    private static final String ENDPOINT = "http://localhost:8080/test-jax-ws-server/HelloWorldImpl";

    public static void main(String[] args) throws RemoteException, ServiceException {
        HelloWorldImplServiceLocator serviceLocator = new HelloWorldImplServiceLocator();
        serviceLocator.setHelloWorldImplPortEndpointAddress(ENDPOINT);
        System.out.println(serviceLocator.getHelloWorldImplPort().getHelloWorldAsString());

    }

}
