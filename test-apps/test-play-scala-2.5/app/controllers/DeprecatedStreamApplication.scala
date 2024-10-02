package controllers

import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.iteratee.Enumerator
import play.api.mvc.Action
import play.api.mvc.Controller

class DeprecatedStreamApplication extends Controller {
  val stringChunk = "lolololololol"
  val chunkEnumerator = Enumerator.unfold(0) { i => if (i < 100) { Thread.sleep(100); Some(i + 1, stringChunk) } else { None } }
  
  def feed = Action {
    Ok.feed(chunkEnumerator)
  }
  
  def streamChunked = Action {
    Ok.chunked(chunkEnumerator)
  }
}