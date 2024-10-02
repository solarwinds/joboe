package com.appoptics.test

import scala.concurrent._
import scala.concurrent.duration._
import scala.collection.mutable._

import java.util.concurrent.TimeUnit
import com.appoptics.api.ext.AgentChecker
import com.appoptics.api.ext.Trace
import com.appoptics.api.ext.TraceContext


object Test extends App {
  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global 
  
  def futureFunction(in : Long) = Future {
    Trace.createEntryEvent("child").report()
    println(in + " child started with " + Trace.getCurrentXTraceID  + " thread " + Thread.currentThread().getId)
    TimeUnit.SECONDS.sleep(1)
    println(in + " child ending with " + Trace.getCurrentXTraceID  + " thread " + Thread.currentThread().getId)
    Trace.createExitEvent("child").report()
    println(in + " child ended with " + Trace.getCurrentXTraceID  + " thread " + Thread.currentThread().getId)
    in * 2
  }

  def submitJob(i: Long) : Future[Long] = {
    val layer = "scala"
    TraceContext.clearDefault()
    val entryEvent = Trace.startTrace(layer)
    val operationName = "test-op-name"
    entryEvent.setAsync()
    entryEvent.report()
    Trace.setTransactionName(operationName)
    println(i + " start " + Thread.currentThread().getId)
    for {
      f <- futureFunction(i)
    } yield {
      println(i + " finished by thread " + Thread.currentThread().getId)
      Trace.endTrace(layer)
      println(i + "end " + Thread.currentThread().getId)
      f
    }
  }
  
  AgentChecker.waitUntilAgentReady(5, TimeUnit.SECONDS)
  
  val futures = ListBuffer[Future[Long]]()
  for (i <- 0 until 50) {
    futures.append(submitJob(i))
  }



  println(Await.result(Future.sequence(futures.toList), Duration(60, TimeUnit.SECONDS)))
}






