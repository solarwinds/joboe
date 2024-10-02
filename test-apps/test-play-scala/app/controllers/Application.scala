package controllers

import play.api._
import play.api.mvc._
import play.api.Play.current
import play.utils.Threads
import play.api.libs.ws.WS
import  scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import play.api.libs.iteratee.{Iteratee, Enumerator}
import scala.concurrent.Await
import scala.concurrent.duration._
import play.api.db.DB
import java.util.concurrent.TimeUnit

object Application extends Controller {

  def index = Action {
    val future = Future {
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
    }
    
    Ok(views.html.index(Await.result(future, 10.second)))
  }


  def async = Action.async {
     req =>
       Future {
         Thread sleep 1000
         Ok(views.html.index("Async!!!!"))
       }
  }


  def file = Action {
    Ok.sendFile(new java.io.File("public/images/fish.jpg"))
  }

  def chunkedFile = Action {
    val dataContent: Enumerator[Array[Byte]] = Enumerator.fromFile(new java.io.File("public/images/fish.jpg"))
    Ok.chunked(dataContent)
  }

  def exception = Action {
    throw new RuntimeException("Testing exception")
    InternalServerError(views.html.index("Exception"))
  }

  def doWait(length:Int) = Action {
    request =>
    Thread.sleep(length)
    Ok(views.html.index("Waited for " + length + "(ms)"))
  }

  def webSocket = WebSocket.using[String] { request =>

    // Log events to the console
    val in = Iteratee.foreach[String](println).map { _ =>
      println("Disconnected")
    }

    // Send a single 'Hello!' message
    val out = Enumerator("Hello!")

    (in, out)
  }

  def post = Action {
    request =>
      val values = request.body.asFormUrlEncoded.getOrElse(Map()).get("test")
      values.fold(Ok(views.html.post("Please input value and press enter")))(values => Ok(views.html.post("Received " + values(0))))
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
}
