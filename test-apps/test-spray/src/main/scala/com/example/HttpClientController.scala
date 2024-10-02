package com.example

import spray.routing.HttpServiceActor
import scala.util.{Success, Failure}
import scala.concurrent.duration._
import akka.actor.ActorSystem
import akka.pattern.ask
import akka.event.Logging
import akka.io.IO
import spray.json.{JsonFormat, DefaultJsonProtocol}
import spray.can.Http
import spray.httpx.SprayJsonSupport
import spray.client.pipelining._
import spray.util._
import spray.http._
import spray.client.pipelining._
import MediaTypes._
import scala.concurrent.Future
import akka.util.Timeout
import scala.concurrent.Await
import java.util.concurrent.TimeUnit
import java.net.URL
import java.io.BufferedInputStream
import java.io.InputStreamReader
import java.io.BufferedReader


/**
 * @author pluk
 */
class HttpClientController extends HttpServiceActor {
  case class Elevation(location: Location, elevation: Double)
  case class Location(lat: Double, lng: Double)
  case class GoogleApiResult[T](status: String, results: List[T])
  implicit val system = ActorSystem("simple-spray-client")
  import system.dispatcher // execution context for futures below

object ElevationJsonProtocol extends DefaultJsonProtocol {
  implicit val locationFormat = jsonFormat2(Location)
  implicit val elevationFormat = jsonFormat2(Elevation)
  implicit def googleApiResultFormat[T :JsonFormat] = jsonFormat2(GoogleApiResult.apply[T])
}
      
   def receive = runRoute(googleRoute ~ crossHostRouteSpray ~ crossHostRoutePlain)
 
  // some sample routes
 val googleRoute = {
    path("google") {
      get { requestContext =>
        import ElevationJsonProtocol._
        import SprayJsonSupport._
        val pipeline = sendReceive ~> unmarshal[GoogleApiResult[Elevation]]
      
        val responseFuture = pipeline {
          Get("http://maps.googleapis.com/maps/api/elevation/json?locations=27.988056,86.925278&sensor=false")
        }
        
        responseFuture.onComplete {
          case Success(GoogleApiResult(_, Elevation(_, elevation) :: _)) =>
            requestContext.complete(String.valueOf(elevation))
          case _ =>
            requestContext.complete("Couldn't get elevation")
        }
      }
    }
  }
 val crossHostRouteSpray = {
   path("cross-host-spray") {
     get {
       respondWithMediaType(`text/html`) { requestContext =>
         implicit val implicitTimeout : Timeout = Timeout(100, TimeUnit.SECONDS)
         val pipeline: Future[SendReceive] =
          for (
            Http.HostConnectorInfo(connector, _) <-
              IO(Http) ? (Http.HostConnectorSetup("localhost", port = 8081))
          ) yield sendReceive(connector)
        
          val request = Get("/")
          pipeline.onSuccess{
            case sendReceive : SendReceive => 
              val response: Future[HttpResponse] = sendReceive(request)
              requestContext.complete(Await.result(response.map { _.entity.asString }, Duration.Inf))  
          }
        }
     }
   }
 }
 val crossHostRoutePlain = {
   path("cross-host-plain") {
     get { requestContext =>
        implicit val implicitTimeout : Timeout = Timeout(100, TimeUnit.SECONDS)
        val url = new URL("http://localhost:8081")
        val connection = url.openConnection()
        val reader = new BufferedReader(new InputStreamReader(connection.getInputStream))
        
        val stringBuffer = new StringBuffer()
        var line : String = reader.readLine()
        while (line != null) {
          stringBuffer.append(line)
          line = reader.readLine()
        }
        reader.close()
        requestContext.complete(stringBuffer.toString())
     }
   }
 }
 
 
}

 