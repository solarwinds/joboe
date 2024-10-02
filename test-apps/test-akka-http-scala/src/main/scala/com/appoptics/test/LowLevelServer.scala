package com.appoptics.test

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer

import scala.concurrent.{ExecutionContext, Future}
import scala.io.StdIn
 
object LowLevelServer {
  def bind(host: String, port : Int)(implicit system : ActorSystem, materializer : ActorMaterializer, executionContext : ExecutionContext) : Future[ServerBinding] = {
    val requestHandler: HttpRequest => HttpResponse = { r =>
      println(s"request: $r")
      r match {
        case HttpRequest(_, Uri.Path("/"), headers, requestEntity: RequestEntity, _) =>

          HttpResponse(entity = HttpEntity(
            ContentTypes.`text/html(UTF-8)`,
            "<html><body>Hello world!</body></html>"))

        case HttpRequest(GET, Uri.Path("/ping"), _, _, _) =>
          println("ping received")
          HttpResponse(entity = "PONG!")

        case HttpRequest(GET, Uri.Path("/crash"), _, _, _) =>
          sys.error("BOOM!")

        case r: HttpRequest =>
          r.discardEntityBytes() // important to drain incoming HTTP Entity stream
          HttpResponse(404, entity = "Unknown resource!")
      }
    }

    val bindingFuture = Http().bindAndHandleSync(requestHandler, host, port)
    println(s"Low Level API Server online at http://$host:$port/")
    bindingFuture
  }
}