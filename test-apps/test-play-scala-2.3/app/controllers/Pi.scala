package controllers

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import java.util.concurrent.TimeUnit
import scala.concurrent.Future
import scala.concurrent.Promise
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.util.Success
import scala.concurrent.ExecutionContext.Implicits.global
import com.appoptics.api.ext.Trace
import com.appoptics.api.ext.TraceContext
import akka.routing.RoundRobinPool
import scala.concurrent.ExecutionContext
import akka.stream.scaladsl._
import akka.stream.ActorMaterializer

/**
 * @author pluk
 */
object Pi {
  implicit val timeout = Timeout(5000, TimeUnit.MILLISECONDS)
  //calculate(nrOfWorkers = 4, nrOfElements = 10000, nrOfMessages = 10000)

  // actors and messages ...

  def calculate(nrOfWorkers: Int, nrOfElements: Int, nrOfMessages: Int)(implicit executor: ExecutionContext): Double = {
    // Create an Akka system
    val system = ActorSystem("PiSystem", None, None, Some(executor))
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
  implicit val materializer = ActorMaterializer()

  //  def test(nrOfWorkers: Int, nrOfElements: Int, nrOfMessages: Int)(implicit executor : ExecutionContext) : Unit = {
  //    val workMessages = Source(1 to nrOfMessages)
  //    val g = FlowGraph.closed() { implicit b =>
  //      import FlowGraph.Implicits._
  //      
  //     val tweets = Source(1 to 10)
  //      
  //      val bcast = b.add(Broadcast[Int](2))
  //      val merge = Merge[Int](2)
  //      tweets ~> bcast.in
  //      bcast.out(0) ~> Flow[Int].map(_ + 1) ~> merge ~> Sink.ignore 
  //      bcast.out(1) ~> Flow[Int].map(_ + 2) ~> merge
  //    }
  //  }

  //  def balancer[In, Out](worker: Flow[In, Out, Unit], workerCount: Int): Flow[In, Out, Unit] = {
  //  import FlowGraph.Implicits._
  // 
  //    Flow() { implicit b =>
  //      val balancer = b.add(Balance[In](workerCount, waitForAllDownstreams = true))
  //      val merge = b.add(Merge[Out](workerCount))
  //   
  //      for (_ <- 1 to workerCount) {
  //        // for each worker, add an edge from the balancer to the worker, then wire
  //        // it to the merge element
  //        balancer ~> worker ~> merge
  //      }
  //   
  //      (balancer.in, merge.out)
  //    }
  //  }

  def calculateWithStream(nrOfWorkers: Int, nrOfElements: Int, nrOfMessages: Int)(implicit executor: ExecutionContext): Double = {
    var pi = 0.0
    val workInterator = Source(0 until nrOfMessages)

    val sink = Sink.foreach { partialResult: Double => pi += partialResult }

    val g = FlowGraph.closed(sink) { implicit b =>
      sink =>
        import FlowGraph.Implicits._

        val balancer = b.add(Balance[Work](nrOfWorkers))
        val merge = b.add(Merge[Double](nrOfWorkers))

        workInterator ~> Flow[Int].map(workIndex => Work(workIndex * nrOfElements, nrOfElements)) ~> balancer.in
        for (i <- 0 until nrOfWorkers) {
          balancer ~> Flow[Work].map(work => calculatePiFor(work.start, work.nrOfElements)) ~> merge
        }
        merge ~> sink
    }

    Await.ready(g.run(), Duration(10, TimeUnit.SECONDS))
    pi
  }

  def calculatePiFor(start: Int, nrOfElements: Int): Double = {
    //Trace.createEntryEvent("test-api").report
    var acc = 0.0
    for (i <- start until (start + nrOfElements))
      acc += 4.0 * (1 - (i % 2) * 2) / (2 * i + 1)

//    val event = Trace.createExitEvent("test-api")
//    event.addBackTrace()
//    event.report()
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
    Props[Worker].withRouter(RoundRobinPool(nrOfWorkers)), name = "workerRouter")

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
//      println("\n\tPi approximation: \t\t%s\n\tCalculation time: \t%s"
//        .format(pi, duration))
      promise.complete(Success(pi))
      context.system.shutdown()
  }
}