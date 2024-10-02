package controllers

import java.util.concurrent.TimeUnit

import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.Promise
import scala.concurrent.duration.Duration
import scala.util.Success

import akka.actor._
import akka.pattern.ask
import akka.routing.RoundRobinRouter
import akka.util.Timeout

/**
 * @author pluk
 */
object Pi {
  implicit val timeout = Timeout(5000, TimeUnit.MILLISECONDS)
  //calculate(nrOfWorkers = 4, nrOfElements = 10000, nrOfMessages = 10000)

  // actors and messages ...

  def calculate(nrOfWorkers: Int, nrOfElements: Int, nrOfMessages: Int): Double = {
    // Create an Akka system
    val system = ActorSystem("PiSystem")
    val promise = Promise[Double]
    // create the result listener, which will print the result and shutdown the system
    val listener = system.actorOf(Props(new Listener(promise)), name = "listener")
    
    // create the master
    val master = system.actorOf(Props(new Master(
      nrOfWorkers, nrOfMessages, nrOfElements, listener)),
      name = "master")

    // start the calculation
    master ! Calculate
    Await.result(promise.future, Duration(100, TimeUnit.SECONDS))
  }

  implicit val system = ActorSystem("reactive-tweets")

    def calculatePiFor(start: Int, nrOfElements: Int): Double = {
    //Trace.createEntryEvent("test-api").report
    var acc = 0.0
    for (i <- start until (start + nrOfElements))
      acc += 4.0 * (1 - (i % 2) * 2) / (2 * i + 1)
    acc
  }
}

sealed trait PiMessage
case object Calculate extends PiMessage
case class Work(start: Int, nrOfElements: Int) extends PiMessage
case class Result(value: Double) extends PiMessage
case class PiApproximation(pi: Double, duration: Duration)

class Worker extends Actor {
  def receive = {
    case Work(start, nrOfElements) =>
      sender ! Result(Pi.calculatePiFor(start, nrOfElements)) // perform the work
  }
}

class Master(nrOfWorkers: Int, nrOfMessages: Int, nrOfElements: Int, listener: ActorRef)
    extends Actor {

  var pi: Double = _
  var nrOfResults: Int = _
  val start: Long = System.currentTimeMillis

  val workerRouter = context.actorOf(
    Props[Worker].withRouter(RoundRobinRouter(nrOfWorkers)), name = "workerRouter")

  def receive = {
    case Calculate =>
      for (i <- 0 until nrOfMessages) workerRouter ! Work(i * nrOfElements, nrOfElements)
    case Result(value) =>
      pi += value
      nrOfResults += 1
      if (nrOfResults == nrOfMessages) {
        // Send the result to the listener
        listener ! PiApproximation(pi, duration = Duration(System.currentTimeMillis - start, TimeUnit.MILLISECONDS))
        // Stops this actor and all its supervised children
        context.stop(self)
      }
  }
}

class Listener(promise: Promise[Double]) extends Actor {
  def receive = {
    case PiApproximation(pi, duration) =>
      println("\n\tPi approximation: \t\t%s\n\tCalculation time: \t%s"
        .format(pi, duration))
      promise.complete(Success(pi))
      context.system.shutdown()
  }
}