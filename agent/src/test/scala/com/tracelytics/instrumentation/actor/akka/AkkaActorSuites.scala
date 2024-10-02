package com.tracelytics.instrumentation.actor.akka

import scala.collection.immutable.Map
import org.scalatest.BeforeAndAfterAll
import org.scalatest.Finders
import org.scalatest.Matchers
import org.scalatest.WordSpecLike
import org.scalatest.Suites
import org.scalatest.FlatSpec
import scala.collection.mutable.Stack
import scala.collection.mutable.Set
import akka.actor.ActorSystem
import com.tracelytics.instrumentation.base.InstrumentationTest
import akka.actor.Props
import com.tracelytics.instrumentation.actor.akka._
import scala.concurrent.Promise
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.collection.JavaConversions._
import akka.pattern.{ask}
import akka.util.Timeout
import java.util.concurrent.TimeUnit
import com.tracelytics.joboe.Metadata
import com.tracelytics.joboe.Context
import scala.collection.mutable.ListBuffer
import scala.concurrent._
import ExecutionContext.Implicits.global

class AkkaActorSuites extends Suites(
  new AkkaActorCellInstrumentationSpec
)


class AkkaActorCellInstrumentationSpec extends InstrumentationTest with BeforeAndAfterAll {
  implicit val timeout = Timeout(5, TimeUnit.SECONDS)
  //def this() = this(ActorSystem("MySpec"))
  var system : ActorSystem = null
  
  
  override def beforeEach() {
    system = ActorSystem("MySpec")
    super.beforeEach()
  }
  
  override def afterEach() {
    system.shutdown()
    super.afterEach()
  }
  
  "A AkkaActorCellInstrumenation" should "capture receive activity on the actor" in {
    val promise = Promise[Set[Thread]]
    val master = system.actorOf(Props[AkkaMaster], "my-akka-master")
    Await.ready(master ? Ping(), Duration.Inf)
    Thread.sleep(1000) //wait for the above to finish. Even if receive finishes, invoke() might not...
    
    val sentEvents = getSentEvents()
    
    assert(sentEvents.length == 2)
    val layerEntryEvent = sentEvents(0).getSentEntries 
    assert(layerEntryEvent("Label") == "entry")
    assert(layerEntryEvent("Layer") == "akka-actor")
    assert(layerEntryEvent("Message").asInstanceOf[String].contains("(com.tracelytics.instrumentation.actor.akka.Ping)"))
    assert(layerEntryEvent.containsKey("MessageWaitTime"))
    
    
    val layerExitEvent = sentEvents(1).getSentEntries 
    assert(layerExitEvent("Label") == "exit")
  }
  
  it should "capture asynchronous operations on child actors with correct edges and clear up context after" in {
    val promise = Promise[Set[Thread]]
    val master = system.actorOf(Props[AkkaMaster], "my-akka-master")
    
    //1 master, 1 job, 5 workers, 10 work units
    master ! (Start(1, promise))
    
    val workerThreads = Await.result(promise.future, Duration.Inf) //make sure last method returns
    
    Thread.sleep(1000) //wait for the above to finish. Even if receive finishes, invoke() might not...
    
    val sentEvents = getSentEvents().map { _.getSentEntries }
    
    //each message receive triggers 2 events
    //total messages = 1 for master Start, 10 for child WORK, 10 for child DONE = 21 messages
    //total events = total messages * 2 events = 42 events  
    assert(sentEvents.length == 42)
    
    val masterStartEntryXTraceId= sentEvents(0)("X-Trace").asInstanceOf[String]
    val opId = new Metadata(masterStartEntryXTraceId).opHexString()
    
    //should have 11 events pointing to this. The exit of the master start, and 10 of the router child entries
    val eventsPointingAtStartEntry = sentEvents.filter { entries =>
      opId == entries.get("Edge")
    }
    assert(eventsPointingAtStartEntry.size == 11)
    
    //ensure the context are cleared up
    workerThreads.foreach { workerThread =>
      val isClear = isThreadLocalMetadataCleared(workerThread)
      if (!isClear) {
        println("[1]worker thread not clear, id : " + workerThread.getId + " name : " + workerThread.getName + " full : " + workerThread + " current thread : " + Thread.currentThread().getId)
      }
      assert(isClear)
    }
  }
  
  it should "capture asynchronous operations on child actors with correct edges and clear up context after (highly asynchronous)" in {
    
    val master = system.actorOf(Props[AkkaMaster], "my-akka-master")
    val promises = ListBuffer[Promise[Set[Thread]]]()
    //1 master, 10 job, 5 workers, 100 work units
    for (jobId <- 0 until 10) {
      val promise = Promise[Set[Thread]]
      promises += promise
      master ! (Start(jobId, promise))
    }
    
    val workerThreads = Await.result(Future.sequence(promises.map { _.future }), Duration.Inf).flatten 
    //val workerThreads = Await.result(promise.future, Duration.Inf) //make sure last method returns
    
    Thread.sleep(1000) //wait for the above to finish. Even if receive finishes, invoke() might not...
    
    val sentEvents = getSentEvents().map { _.getSentEntries }
    
    //each message receive triggers 2 events
    //total messages for each job = 1 for master Start, 10 for child WORK, 10 for child DONE = 21 messages
    //total events = total messages for each job * 10 * 2 events = 420 events  
    assert(sentEvents.length == 420)
    
    val masterStartEntryXTraceId= sentEvents(0)("X-Trace").asInstanceOf[String]
    val opId = new Metadata(masterStartEntryXTraceId).opHexString()
    
    //should have 11 events pointing to this. The exit of the master start, and 10 of the router child entries
    val eventsPointingAtStartEntry = sentEvents.filter { entries =>
      opId == entries.get("Edge")
    }
    assert(eventsPointingAtStartEntry.size == 11)
    
    //ensure the context are cleared up
    workerThreads.foreach { workerThread => 
      val isClear = isThreadLocalMetadataCleared(workerThread)
      if (!isClear) {
        println("[2]worker thread not clear, id : " + workerThread.getId + " name : " + workerThread.getName + " class : " + workerThread.getClass.getName + " full : " + workerThread + " current thread : " + Thread.currentThread().getId)
      }
      assert(isClear)
    }
    isThreadLocalMetadataCleared(Thread.currentThread())
  }
  it should "only capture extents for actors that match the name pattern defined yet propagate context for all" in {
    val promise = Promise[Set[Thread]]
    val master = system.actorOf(Props[AkkaMaster], "your-akka-master") //name that does not match the scalaagent.json
    
    //1 master, 1 job, 5 workers, 10 work units
    master ! (Start(1, promise))
    
    val workerThreads = Await.result(promise.future, Duration.Inf) //make sure last method returns
    
    Thread.sleep(1000) //wait for the above to finish. Even if receive finishes, invoke() might not...
    
    val sentEvents = getSentEvents().map { _.getSentEntries }
    
    //each message receive triggers 2 events
    //total messages = 10 for child WORK (take note that START, DONE sent to master should not be captured due to master name unmatched)
    //total events = total messages * 2 events = 20 events  
    assert(sentEvents.length == 20)
    
    //ensure the context are cleared up
    workerThreads.foreach { workerThread =>
      val isClear = isThreadLocalMetadataCleared(workerThread)
      if (!isClear) {
        println("[3]worker thread not clear, id : " + workerThread.getId + " name : " + workerThread.getName + " class : " + workerThread.getClass.getName + " full : " + workerThread + " current thread : " + Thread.currentThread().getId)
      }
      assert(isClear)    
    }
    
  }
  
  def isThreadLocalMetadataCleared(thread : Thread) : Boolean = {
    val field = classOf[Context].getDeclaredField("mdThreadLocal")
    field.setAccessible(true)
    val mdThreadLocal = field.get(null).asInstanceOf[ThreadLocal[Metadata]]
    getThreadLocalValue(mdThreadLocal, thread) match {
      case Some(metadata) =>
        !metadata.isValid()
      case None => //shouldn't be none?
        true
    }
  }
  
  
  def getThreadLocalValue[T](threadLocal : ThreadLocal[T], thread : Thread) : Option[T] = {
    val field = classOf[Thread].getDeclaredField("threadLocals");
    field.setAccessible(true);
    val threadLocalmap = field.get(thread);
    
    val method = Class.forName("java.lang.ThreadLocal$ThreadLocalMap").getDeclaredMethod("getEntry", classOf[ThreadLocal[Any]]);
    method.setAccessible(true);
    val entry = method.invoke(threadLocalmap, threadLocal);

    val valueField = Class.forName("java.lang.ThreadLocal$ThreadLocalMap$Entry").getDeclaredField("value");
    valueField.setAccessible(true);
    if (entry != null) {
      Some(valueField.get(entry).asInstanceOf[T])
    } else { //try inheritable Thread local
      val inheritableField = classOf[Thread].getDeclaredField("inheritableThreadLocals");
      inheritableField.setAccessible(true)
      val inhertiableThreadLocalMap = inheritableField.get(thread)
      
      method.setAccessible(true);
      val inheritableEntry = method.invoke(inhertiableThreadLocalMap, threadLocal);
      
      if (inheritableEntry != null) {
        Some(valueField.get(inheritableEntry).asInstanceOf[T])
      } else {
        None
      }
    }
  }
}