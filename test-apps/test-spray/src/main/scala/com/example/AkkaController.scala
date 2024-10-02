package com.example

import spray.routing.HttpServiceActor
import akka.actor.Actor
import spray.routing._
import spray.http._
import spray.httpx.SprayJsonSupport._
import MyJsonProtocol._
import MediaTypes._
import akka.actor.Props
import akka.routing.RoundRobinPool

/**
 * @author pluk
 */
class AkkaController extends HttpServiceActor {
    
      
   def receive = runRoute(aSimpleRoute ~ anotherRoute ~ workerRoute)
 
  // some sample routes
 val aSimpleRoute = {
    path("path1") {
      get {
 
        // Get the value of the content-header. Spray
        // provides multiple ways to do this.
        headerValue({
          case x@HttpHeaders.`Content-Type`(value) => Some(value)
          case default => None
        }) {
          // the header is passed in containing the content type
          // we match the header using a case statement, and depending
          // on the content type we return a specific object
          header => header match {
 
            // if we have this contentype we create a custom response
            case ContentType(MediaType("application/vnd.type.a"), _) => {
              respondWithMediaType(`application/json`) {
                complete {
                  Person("Bob", "Type A", System.currentTimeMillis());
                }
              }
            }
 
            // if we habe another content-type we return a different type.
            case ContentType(MediaType("application/vnd.type.b"), _) => {
              respondWithMediaType(`application/json`) {
                complete {
                  Person("Bob", "Type B", System.currentTimeMillis());
                }
              }
            }
 
            // if content-types do not match, return an error code
            case default => {
              complete {
                HttpResponse(406);
              }
            }
          }
        }
      }
    }
  }
 
  // handles the other path, we could also define these in separate files
  // This is just a simple route to explain the concept
  val anotherRoute = {
    path("path2") {
      get {
        // respond with text/html.
        respondWithMediaType(`text/html`) {
          complete {
            // respond with a set of HTML elements
            <html>
              <body>
                <h1>Path 2</h1>
              </body>
            </html>
          }
        }
      }
    }
  }
  val workerRoute = {
    val akkaMaster = context.actorOf(Props[AkkaMaster], "my-akka-master")
    path("work") {
      get {
        respondWithMediaType(`application/json`) { context =>
          akkaMaster ! Start(context)
        }
      }
    }
  }
}

class ActorMessage
case class Work(id : Int) extends ActorMessage
case class Start(context : RequestContext) extends ActorMessage
case class Done(id : Int) extends ActorMessage
 