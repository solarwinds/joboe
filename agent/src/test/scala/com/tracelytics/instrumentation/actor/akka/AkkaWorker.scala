package com.tracelytics.instrumentation.actor.akka

import akka.actor.Actor
import com.tracelytics.instrumentation.actor.akka._

/**
 * @author pluk
 */
class AkkaWorker extends Actor {
    
      
   def receive = {
     case Work(jobId, workId, isHeavy) => 
       println("Recieved work of id " + workId + " of job " + jobId)
       //lazy worker!
       if (isHeavy) {
         Thread.sleep(100)
       } else {
         Thread.sleep(10)
       }
       sender() ! Done(jobId, workId, Thread.currentThread())
   }
}

