package com.example

import scala.util.{Failure, Success}
import akka.actor.ActorSystem
import akka.event.Logging
import com.appoptics.api.ext.Trace
import scala.concurrent.duration.Duration
import scala.concurrent.Await

object Main extends App
  with ConnectionLevelApiDemo
  with HostLevelApiDemo
  with RequestLevelApiDemo {

  // we always need an ActorSystem to host our application in
  implicit val system = ActorSystem("simple-example")
  import system.dispatcher // execution context for future transformations below
  val log = Logging(system, getClass)

  // the spray-can client-side API has three levels (from lowest to highest):
  // 1. the connection-level API
  // 2. the host-level API
  // 3. the request-level API
  //
  // this example demonstrates all three APIs by retrieving the server-version
  // of http://spray.io in three different ways

  val host = "spray.io"

  val event = Trace.startTrace("trace-spray-client")
  event.addInfo("URL", "main".asInstanceOf[Any])
  event.report

  
  val result = for {
    result1 <- demoConnectionLevelApi(host)
    result2 <- demoHostLevelApi(host)
    result3 <- demoRequestLevelApi(host)
    result4 <- requestLevelUri("https://www.google.ca/services/?fg=1")
    result5 <- repeatedHostLevelRequests("google.com")
 } yield Set(result1, result2, result3, result4, result5)
//  } yield Set(result7)
  
  
//  val result = for {
//    result1 <- demoConnectionLevelApi(host)
//  } yield Set(result1)
  
  result onComplete {
    case Success(res) => log.info("{} is running {}", host, res mkString ", ")
    case Failure(error) => log.warning("Error: {}", error)
  }
  try {
    Await.result(result, Duration.Inf)
  } finally {
    system.shutdown() 
    Trace.endTrace("trace-spray-client")  
  }
   
  
}
