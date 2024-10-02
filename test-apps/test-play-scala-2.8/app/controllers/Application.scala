package controllers

import play.api._
import play.api.mvc._
import play.api.db._
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Try
import java.net.URL
import java.io.InputStream
import scala.util.Failure
import scala.util.Success
import scala.concurrent.Future
import scala.concurrent.Promise
import java.util.concurrent.TimeUnit
import com.appoptics.api.ext.Trace
import com.appoptics.api.ext.TraceContext
import scala.concurrent.ExecutionContext
import java.util.concurrent.Executors
import javax.inject.Inject

class Application @Inject()(db: Database, cc: ControllerComponents)(implicit ec: ExecutionContext) extends AbstractController(cc) {

  def index = Action {
    val futureQ = Future { println("hi") }
    
    
    Try(Await.result(Future.sequence(Seq(getJdbcFuture, getJdbcFuture, getJdbcFuture)), Duration(10, TimeUnit.SECONDS))) match {
      case Success(result) => Ok(views.html.index(result.foldLeft("")(_ + " " + _)))
      case Failure(ex) => Ok(views.html.index(ex.getMessage))
    }
  }
  
  val valAction = Action {
    Ok("action defined as val")
  }

  def async = Action.async {
    getJdbcFuture.map { outString =>
      Thread.sleep(10) //do something...nom.nom.nom
      Ok(views.html.index(outString)) 
    }
  }

  //request with "request" parameter referenced
  def request = Action {
    request => Ok("Reference the request instance : " + request)
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
  
  def exception = Action {
    if (true) {
      throw new RuntimeException("testing exception")
    }
    Ok("Never gets to here")
  }
  
  def delete = Action {
    Ok("done")
  }
  
  def futureException = Action.async {
    Future {
      if (true) {
        throw new RuntimeException("testing exception")
      }
      Ok("Never gets to here")
    }
  }
  
  def actor = Action {
    Ok("Result is " + Pi.calculate(5, 100000, 20))
  }
  
  def actorOtherContext = Action {
    Ok("Result is " + Pi.calculate(5, 100000, 20)(ExecutionContext.fromExecutorService(Executors.newCachedThreadPool())))
  }
  
  private def getJdbcFuture = scala.concurrent.Future {
    var outString = "Number is "
    // access "default" database
    db.withConnection { conn =>
      val stmt = conn.createStatement
      val rs = stmt.executeQuery("SELECT 9 as testkey ")
      while (rs.next()) {
        outString += rs.getString("testkey")
      }
    }
    outString
  }
}
