package com.example

import spray.routing.HttpServiceActor
import akka.actor.Actor
import spray.routing._
import spray.http._
import spray.httpx.SprayJsonSupport._
import MyJsonProtocol._
import MediaTypes._
import com.example.db.HelloSlick
import scala.concurrent.Future
import scala.util.Random

/**
 * @author pluk
 */
class AkkaWorker extends Actor {
    
      
   def receive = {
     case Work(id) => 
//       if (Random.nextBoolean()) { //strike o/
//         throw new WorkerOnStrikeException(id)
//       }
       
       println("Recieved work of id " + id)
       //read db
       HelloSlick.runQuery
       println("SENDER IS (1) " + sender())
       
       sender() ! Done(id)  
   }
}

