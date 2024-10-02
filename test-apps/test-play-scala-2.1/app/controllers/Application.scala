package controllers

import play.api._
import play.api.mvc._
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.libs.concurrent.Akka
import akka.actor.Props
import play.api.Play.current
import play.api.db.DB

object Application extends Controller {
  def index = Action {
    val future = scala.concurrent.Future { 
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
    Async {
      future.map( _ => 
        Ok(views.html.index("Your new application is ready.")))
    }
  }
  
  def actor = Action { 
    println("something")
    println("no")
    Ok("Result is " + Pi.calculate(5, 100000, 20))
  }
}
