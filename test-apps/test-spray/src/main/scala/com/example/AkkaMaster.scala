package com.example

import spray.routing.HttpServiceActor

import akka.actor.Actor
import spray.routing._
import spray.http._
import spray.httpx.SprayJsonSupport._
import MyJsonProtocol._
import MediaTypes._
import com.example.db.HelloSlick
import scala.collection.mutable.Set
import akka.actor.Props
import akka.routing.RoundRobinPool
import akka.actor.OneForOneStrategy
import akka.actor.SupervisorStrategy._
import scala.concurrent.duration._

/**
 * @author pluk
 */
class AkkaMaster extends Actor {
   val router = context.actorOf(Props[AkkaWorker].withRouter(RoundRobinPool(2)), name = "my-akka-router")

   val outstandingIds = Set[Int]()
   var currentRequestContext : Option[RequestContext] = None
   def receive = {
     case Start(context) =>
       currentRequestContext match {
         case Some(requestContext) => 
           requestContext.complete("Still processing another work")  
         case None =>
           println("Master starting!")
           currentRequestContext = Some(context)
           
           HelloSlick.runUpdate
           for (i <- 0 until 3) {
             println("distributing work")
             router ! Work(i)
             outstandingIds += i
           }
                  
       }
     case Done(id) =>
       completeWorker(id)
   }
   
   def completeWorker(id : Int) = {
     outstandingIds -= id
       if (outstandingIds.isEmpty) {
         currentRequestContext.get.complete("Done with Slick call")
         currentRequestContext = None
       }
   }
   
   override val supervisorStrategy =
  OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1 minute) {
    case WorkerOnStrikeException(id) =>
      println("Worker " + id + " on strike...")
      completeWorker(id)
      Resume
  }
   
   
}

