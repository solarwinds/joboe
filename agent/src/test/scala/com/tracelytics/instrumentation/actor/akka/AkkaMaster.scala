package com.tracelytics.instrumentation.actor.akka

import akka.actor.Actor
import scala.collection.mutable.Set
import akka.actor.Props
import akka.routing.RoundRobinPool
import com.tracelytics.instrumentation.actor.akka._
import scala.concurrent.Promise
import scala.collection.mutable.Map
import scala.util.Random

/**
 * @author pluk
 */
class AkkaMaster() extends Actor {
   private val router = context.actorOf(Props[AkkaWorker].withRouter(RoundRobinPool(5)), name = "my-akka-router")

   private val outstandingWorkIds = Map[Int, Set[Int]]()
   private val workerThreads = Map[Int, Set[Thread]]()
   private val promisesByJobId = Map[Int, Promise[Set[Thread]]]()
   
   def receive = {
     case Start(jobId, promise) =>
       println("Master starting for job " + jobId + "!")
       promisesByJobId += (jobId -> promise)
       for (workdId <- 0 until 10) {
         router ! Work(jobId, workdId, Random.nextBoolean())
         outstandingWorkIds.getOrElseUpdate(jobId, Set[Int]()) += workdId
       }
     case Done(jobId, workId, thread) =>
       outstandingWorkIds(jobId) -= workId
       workerThreads.getOrElseUpdate(jobId, Set[Thread]()) += thread
       if (outstandingWorkIds(jobId).isEmpty) {
         println("Done!")
         promisesByJobId(jobId).success(workerThreads.remove(jobId).get)
       }
     case Ping() =>
       sender() ! ("Pong!")
   }
}

