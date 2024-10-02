package com.example

import spray.httpx.marshalling.ToResponseMarshallable.isMarshallable
import spray.routing.Directive.pimpApply
import spray.routing.HttpServiceActor


/**
 * @author pluk
 */
class DummyService extends HttpServiceActor {
      
   def receive = runRoute(route)
 
  // some sample routes
 val route = {
    pathSingleSlash {
      get { 
         complete("dummy" + Math.random())
      }
    }
  }
} 
 