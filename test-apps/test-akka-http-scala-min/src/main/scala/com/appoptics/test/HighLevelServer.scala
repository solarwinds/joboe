package com.appoptics.test

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.ExceptionHandler
import akka.stream.ActorMaterializer

import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object HighLevelServer {
  implicit def myExceptionHandler: ExceptionHandler =
    ExceptionHandler {
      case _: MyException =>
        complete(HttpResponse(StatusCodes.OK, entity = "Handled Exception"))
    }


  def bind(host : String, port : Int)(implicit system : ActorSystem, materializer : ActorMaterializer, executionContext : ExecutionContext) : Future[ServerBinding] = {
    val route =
      path("hello") {
        get {
          extractRequest
          complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>Say hello to akka-http</h1>"))
        }
      } ~
      path("crash") {
        get {
          sys.error("KABOOM!")
        }
      } ~
      path("handled-exception") {
        get {
          throw new MyException("testing exception - handled")
        }
      } ~
      path("cross-server") {
        get {
          parameters('host.?, 'port.?, 'path.?, 'method.?, 'isRelative.?) { (hostO, portO, pathO, methodO, isRelaiveO) =>
            val host = hostO.getOrElse("localhost")
            val port = portO.map(_.toInt).getOrElse(8080)
            val path = pathO.getOrElse(Uri./)
            val isRelative = isRelaiveO.map(_.toBoolean).getOrElse(false)
            val method = methodO.fold(HttpMethods.GET)(methodKey => HttpMethods.getForKey(methodKey.toUpperCase()).getOrElse(HttpMethods.GET))
            val uri = s"http://$host:$port" + path
            //issue 2 requests
            val httpRequest =
              if (isRelative) {
                HttpRequest(uri = Uri(uri).toRelative, method = method).addHeader(headers.Host(host, port)) //relative URI does not really work see https://github.com/akka/akka/issues/20934
              } else {
                HttpRequest(uri = uri, method = method)
              }
            val responsesFuture = Future.sequence(Seq(Http().singleRequest(httpRequest), Http().singleRequest(httpRequest)))
            onComplete(responsesFuture) {
              case Success(responses) =>
                val responseCodes = responses.map(_.status)
                complete(s"Got from $uri response code are ${responseCodes}")
              case Failure(exception) => complete(s"Failed to call $uri, exception message ${exception.getMessage}")
            }
          }
        }
      } ~
      path("wait") {
        get {
          parameter("duration") { duration =>
            TimeUnit.SECONDS.sleep(duration.toLong)
            complete(s"Waited for $duration seconds")
          }
        }
      } ~
      path("timeout") {
        withRequestTimeout(Duration(1, TimeUnit.SECONDS)) { // modifies the global akka.http.server.request-timeout for this request
          val response: Future[String] = Future {
            TimeUnit.SECONDS.sleep(2)
            "too late!"}
          complete(response)
        }
      }




    val bindingFuture = Http().bindAndHandle(route, host, port)

    println(s"High Level API Server online at http://$host:$port/")
    bindingFuture
  }
}

class MyException(message : String) extends RuntimeException
