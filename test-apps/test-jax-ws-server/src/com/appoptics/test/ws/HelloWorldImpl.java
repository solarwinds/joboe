package com.appoptics.test.ws;

import javax.jws.WebService;

//Service Implementation Bean

@WebService(endpointInterface = "com.appoptics.test.ws.HelloWorld")
public class HelloWorldImpl implements HelloWorld{

  @Override
  public String getHelloWorldAsString() {
      return "Hello World JAX-WS";
  }
}