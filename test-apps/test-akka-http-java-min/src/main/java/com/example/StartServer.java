package com.example;

import java.io.IOException;

import com.example.high.HighLevelApi;
import com.example.low.LowLevelApi;

import akka.actor.ActorSystem;

public class StartServer {

    public static void main(String[] args) throws IOException {
        // boot up server using the route as defined below
        ActorSystem system = ActorSystem.create();
        
        LowLevelApi.bind("0.0.0.0", 8080, system);
        HighLevelApi.bind("0.0.0.0", 9000, system);
        
        System.out.println("Type RETURN to exit");
        System.in.read();
        system.terminate();
    }

}
