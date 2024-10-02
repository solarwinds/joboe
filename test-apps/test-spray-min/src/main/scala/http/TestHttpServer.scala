package http

import spray.can.Http
import akka.actor.ActorSystem
import akka.io.IO
import akka.actor.ActorRef
import akka.actor.Props
import akka.actor.Actor
import spray.http.HttpResponse
import spray.http.HttpRequest
import spray.http.Uri
import spray.http.HttpMethods
import akka.io.Tcp.Connected
import akka.io.Tcp.Register
import spray.http.HttpEntity
import spray.http._
import MediaTypes._

/**
 * @author pluk
 */
object TestHttpServer extends App {
  implicit val system = ActorSystem()

  val myListener: ActorRef = system.actorOf(Props[ListenerActor])
  
  def startServer() = {
    IO(Http) ! Http.Bind(myListener, interface = "0.0.0.0", port = 8080)
  }
  
  class ListenerActor extends Actor {
    def receive = {
      case Connected(remote, local) => 
        println("connected with " + remote)
        sender ! Register(self)  
      
      case HttpRequest(HttpMethods.GET, Uri.Path("/ping"), _, _, _) => {
        println("received ping")
        sender ! HttpResponse(entity = "PONG") 
      }
      case HttpRequest(HttpMethods.GET, Uri.Path("/link"), _, _, _) => {
        println("received link")
        sender ! HttpResponse(entity = HttpEntity(`text/html`, "<a href=\"ping\">link to ping!</a>")) 
      }
//      case any => 
//        println(any + " class: " + any.getClass().getName)   
//        sender ! httpresponse(entity = "pong")
//        println("replied")
    }
  }
  
  startServer()

  
//   class ProcessingActor(replyActor : ActorRef)(implicit executionContext : ExecutionContext) extends Actor {
//    def receive = {
//      case IncomingString(body) =>
//        DomainService.expensiveCall(body).map( processedString => {
//          replyActor ! DomainService.classify(processedString)
//        })
//    }
//  }
}
