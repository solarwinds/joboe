package com.example

import scala.util.{Failure, Success}
import akka.actor.ActorSystem
import akka.event.Logging
import com.appoptics.api.ext.Trace
import scala.concurrent.duration.Duration
import scala.concurrent.Await

object NotFoundHost extends App
  with RequestLevelApiDemo {

  // we always need an ActorSystem to host our application in
  implicit val system = ActorSystem("simple-example")
  import system.dispatcher // execution context for future transformations below
  val log = Logging(system, getClass)

  val event = Trace.startTrace("trace-spray-client")
  event.addInfo("URL", "not-found-host".asInstanceOf[Any])
  event.report
  
  val result = for {
    result1 <- requestLevelUri("http://www.invalidhostzommmmggggg.com/")
  } yield Set(result1)
  
  try {
    Await.result(result, Duration.Inf)
  } finally {
    system.shutdown() 
    Trace.endTrace("trace-spray-client")  
  }
}
