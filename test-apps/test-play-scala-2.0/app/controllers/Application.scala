package controllers

import play.api._
import play.api.mvc._

object Application extends Controller {
  
  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }
  
  def actor = Action { 
    println("something")
    println("no")
    Ok("Result is " + Pi.calculate(5, 100000, 20))
  }
}