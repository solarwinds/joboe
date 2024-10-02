package com.appoptics.test

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer

import scala.concurrent.Future
import scala.io.StdIn


object TestServer {
  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher

    val future = Future.sequence(Seq(LowLevelServer.bind("0.0.0.0", 8080), HighLevelServer.bind("0.0.0.0", 9000)))

    println("Press RETURN to stop...")
    StdIn.readLine() // let it run until user presses return

    future.flatMap(serverBindings =>
      Future.sequence(serverBindings.map(_.unbind))
    ).onComplete(_ => system.terminate()) // and shutdown when done
  }
}

