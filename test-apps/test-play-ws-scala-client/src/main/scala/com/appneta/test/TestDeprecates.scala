package com.appneta.test

import play.api.libs.ws.WSClient
import play.api.libs.ws.EmptyBody
import play.api.libs.iteratee.Iteratee
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object TestDeprecates extends HttpClientTestApp {

  override def test(wsClient: WSClient, url: String) = {
    var futures = Seq[Future[Any]]()
    
    futures = futures :+ (wsClient.url(url).streamWithEnumerator())
    val iteratee = Iteratee.foreach[Array[Byte]] { chunk =>
      val chunkString = new String(chunk, "UTF-8")
      println(chunkString)
    }
    
    futures = futures :+ wsClient.url(url).getStream()
    futures = futures :+ wsClient.url(url).patchAndRetrieveStream("") { _ => iteratee }
    futures = futures :+ wsClient.url(url).postAndRetrieveStream("") { _ => iteratee }
    futures = futures :+ wsClient.url(url).putAndRetrieveStream("") { _ => iteratee }
    
    Future.sequence(futures)
  }
}