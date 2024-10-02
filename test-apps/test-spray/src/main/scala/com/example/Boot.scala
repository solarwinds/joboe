package com.example

import akka.actor.{ActorSystem, Props}
import akka.io.IO
import spray.can.Http
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import kamon.Kamon

object Boot extends App {
  //Kamon.start()
  // we need an ActorSystem to host our application in
  implicit val system = ActorSystem("on-spray-can")

  // create and start our service actor
  val service = system.actorOf(Props[DemoServiceActor], "demo-service")
  val dummyService = system.actorOf(Props[DummyService], "dummy-service")
  


  implicit val timeout = Timeout(5.seconds)
  // start a new HTTP server on port 8080 with our service actor as the handler
  IO(Http) ? Http.Bind(service, interface = "0.0.0.0", port = 8080)
  IO(Http) ? Http.Bind(dummyService, interface = "0.0.0.0", port = 8081)
}
