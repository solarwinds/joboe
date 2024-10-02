package com.tracelytics.instrumentation.actor.akka

import scala.concurrent.Promise
import scala.collection.mutable.Set

/**
 * @author pluk
 */
class ActorMessage
case class Work(jobId : Int, workId : Int, isHeavy : Boolean) extends ActorMessage
case class Start(jobId : Int, promise : Promise[Set[Thread]]) extends ActorMessage
case class Ping() extends ActorMessage
case class Done(jobId : Int, workId : Int, workerThread : Thread) extends ActorMessage