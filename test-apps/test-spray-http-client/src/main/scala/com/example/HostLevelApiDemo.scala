package com.example

import scala.concurrent.Future
import scala.concurrent.duration._
import akka.actor.ActorSystem
import akka.util.Timeout
import akka.pattern.ask
import akka.io.IO
import spray.can.Http
import spray.http._
import HttpMethods._

trait HostLevelApiDemo {
  private implicit val timeout: Timeout = 15.seconds

  // With the host-level API you ask the spray-can HTTP infrastructure to setup an
  // "HttpHostConnector" for you, which is an entity that manages a pool of connection to
  // one particular host. Once set up you can send the host connector HttpRequest instances,
  // which it will schedule across a connection from its pool (according to its configuration)
  // and deliver the responses back to the request sender

  def demoHostLevelApi(host: String)(implicit system: ActorSystem): Future[ProductVersion] = {
    import system.dispatcher // execution context for future transformations below
    for {
      Http.HostConnectorInfo(hostConnector, _) <- IO(Http) ? Http.HostConnectorSetup(host, port = 80)
      response <- hostConnector.ask(HttpRequest(GET, "/")).mapTo[HttpResponse]
      _ <- hostConnector ? Http.CloseAll
    } yield {
      system.log.info("Host-Level API: received {} response with {} bytes",
        response.status, response.entity.data.length)
      response.header[HttpHeaders.Server].get.products.head
    }
  }
  
  def repeatedHostLevelRequests(host:String)(implicit system: ActorSystem): Future[ProductVersion] = {
    import system.dispatcher
    
    val connectorInfoFuture = IO(Http) ? Http.HostConnectorSetup(host, port = 80, sslEncryption = false)
    connectorInfoFuture.flatMap {  
      case Http.HostConnectorInfo(hostConnector, _) =>
        var futures = List[Future[HttpResponse]]()
        for (i <- 0 until 5) {
          futures = hostConnector.ask(HttpRequest(GET, "/")).mapTo[HttpResponse] :: futures 
        }
        
        val resultFuture = Future.sequence(futures).map { responses => 
          hostConnector ? Http.CloseAll
          
          system.log.info("Repeated requests: received {} responses with {} bytes", responses.length, responses.map { _.entity.data.length }.sum)
          
          //just take the first one
          responses.head.header[HttpHeaders.Server].get.products.head
        }
        resultFuture
    }
    
  }

}