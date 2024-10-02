package controllers

import play.api._
import play.api.mvc._
import play.api.Play.current
import play.api.db._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Try
import java.net.URL
import java.io.InputStream
import scala.util.Failure
import scala.util.Success
import scala.concurrent.Future
import play.libs.F.Function0
import scala.concurrent.Promise
import java.util.concurrent.TimeUnit
import com.appoptics.api.ext.Trace
import com.appoptics.api.ext.TraceContext
import scala.concurrent.ExecutionContext
import java.util.concurrent.Executors
import play.api.libs.ws.WS

object Application extends Controller {

  def index = Action {
    Try(Await.result(Future.sequence(Seq(getJdbcFuture, getJdbcFuture, getJdbcFuture)), Duration(10, TimeUnit.SECONDS))) match {
      case Success(result) => Ok(views.html.index(result.foldLeft("")(_ + " " + _)))
      case Failure(ex) => Ok(views.html.index("Time out"))
    }
  }
  
  val hello = Action.async {
    request =>
//      val entryEvent = Trace.createEntryEvent("test-api")
//      entryEvent.addBackTrace()
//      entryEvent.setAsync()
//      entryEvent.report()
      var outString = ""
    DB.withConnection { conn =>
      val stmt = conn.createStatement
      val rs = stmt.executeQuery("SELECT 9 as testkey ")
      while (rs.next()) {
        outString += rs.getString("testkey")
      }
    }
    println(outString)

      WS.url(request.getQueryString("url").get).get().map { r =>
        if (r.status == 200) {
//          val exitEvent = Trace.createExitEvent("test-api")
//          exitEvent.addBackTrace()
//          exitEvent.report() 
          Ok("The website is up") 
        } else { 
//          val exitEvent = Trace.createExitEvent("test-api")
//          exitEvent.addBackTrace()
//          exitEvent.report()
          NotFound("The website is down") 
        }  
      }
    }
  
  def helloSimple = Action.async {
//      val entryEvent = Trace.createEntryEvent("test-api")
//      entryEvent.addBackTrace()
//      entryEvent.setAsync()
//      entryEvent.report()
      
      Future {
        var outString = ""
        DB.withConnection { conn =>
          val stmt = conn.createStatement
          val rs = stmt.executeQuery("SELECT 9 as testkey ")
          while (rs.next()) {
            outString += rs.getString("testkey")
          }
        }
//        Thread.sleep(5000)
        println("returning result for Future")
        
        outString
      }.map { 
//        val exitEvent = Trace.createExitEvent("test-api")
//        exitEvent.addBackTrace()
//        exitEvent.report() 
        Ok(_) 
      }
   }
  
    
  def async = Action.async {
    var outString = ""
    DB.withConnection { conn =>
      val stmt = conn.createStatement
      val rs = stmt.executeQuery("SELECT 9 as testkey ")
      while (rs.next()) {
        outString += rs.getString("testkey")
      }
    }
    println(outString)
    
    getJdbcFuture.map { outString =>
      Thread.sleep(10) //do something...nom.nom.nom
      Ok(outString) 
    }
  }

  def promise = Action {
    val promise = scala.concurrent.Promise[String]
    promise.completeWith(getJdbcFuture)
    
    Try(Await.result(promise.future, Duration(1, TimeUnit.SECONDS))) match {
      case Success(result) => Ok(result)
      case Failure(_) => Ok("Exception")
    }
  }
  
  def api = Action {
    val event =  Trace.createEntryEvent("test-api")
    event.report()
    
    val future = getJdbcFuture
    
    future.onComplete { _ => 
      val event = Trace.createExitEvent("test-api")
      event.report()
    }
    
    //Await.result(future, Duration(10, TimeUnit.SECONDS))
    Thread.sleep(20) //do something...nom.nom.nom
    
    Ok("done!")
  }
  
  def apiHack = Action {
    val existingContext = TraceContext.getDefault 
    val event =  Trace.continueTrace("test-api", Trace.getCurrentXTraceID)
    event.report()
    val newContext = TraceContext.getDefault
    existingContext.setAsDefault() //create a fork
    
    val future = getJdbcFuture
    
    future.onComplete { _ => 
      newContext.setAsDefault() //continue on the fork instead
      val event = Trace.createExitEvent("test-api")
      event.setAsync()
      event.report()
    }
    
    //Await.result(future, Duration(10, TimeUnit.SECONDS))
    Thread.sleep(20) //do something...nom.nom.nom
    
    Ok("done with hack!")
    
  }
  
  def helper = Helper.getAction //this will not be instrumented
  
  def actor = Action {
    Ok("Result is " + Pi.calculate(5, 100000, 20))
  }
  
  def stream = Action {
    Ok("Result is " + Pi.calculateWithStream(5, 100000, 20))
  }
  
  def actorOtherContext = Action {
    Ok("Result is " + Pi.calculate(5, 100000, 20)(ExecutionContext.fromExecutorService(Executors.newCachedThreadPool())))
  }
  
  def remoteActor = Action {
    LocalActor.test()
    Ok("Finsihed invoking remote actor!")
  }
  
  def otherContext = Action {
    val otherContext = ExecutionContext.fromExecutorService(Executors.newCachedThreadPool())
    Try(Await.result(Future.sequence(Seq(getJdbcFuture, getJdbcFuture(otherContext), getJdbcFuture(otherContext))), Duration(10, TimeUnit.SECONDS))) match {
      case Success(result) => Ok(views.html.index(result.foldLeft("")(_ + " " + _)))
      case Failure(ex) => Ok(views.html.index("Time out"))
    }
  }
  
  def exception = Action {
    if (true) {
      throw new RuntimeException("testing exception")
    }
    Ok("Never gets to here")
  }
  
  def futureException = Action.async {
    Future {
      if (true) {
        throw new RuntimeException("testing exception")
      }
      Ok("Never gets to here")
    }
  }

  private def getJdbcFuture(implicit executor : ExecutionContext) = scala.concurrent.Future {
    var outString = "Number is "
    // access "default" database
    DB.withConnection { conn =>
      val stmt = conn.createStatement
      val rs = stmt.executeQuery("SELECT 9 as testkey ")
      while (rs.next()) {
        outString += rs.getString("testkey")
      }
    }
    outString
  }(executor)
}
