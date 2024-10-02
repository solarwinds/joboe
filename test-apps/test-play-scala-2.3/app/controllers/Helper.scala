package controllers

import play.api._
import play.api.mvc._

object Helper {
  def getAction = Action {
     Results.Ok(views.html.index("From helper"))
  }
}