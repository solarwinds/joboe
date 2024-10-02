package controllers

import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.duration.Duration

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import javax.inject.Inject
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.iteratee.Iteratee
import play.api.libs.ws.WS
import play.api.libs.ws.WSClient
import play.api.mvc.Action
import play.api.mvc.Controller

class DeprecatedServiceApplication @Inject() (ws: WSClient) extends Controller {
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  
  def crossServer = Action {
    val testServerUrl = "http://localhost:8080/test-rest-server/"
    
    Await.result(WS.url(testServerUrl).get(), Duration.Inf) //use default client
    Ok("Done")
  }
  
  def getStream = Action {
    val testServerUrl = "http://localhost:8080/test-rest-server/"
    
    val futureResponse =WS.url(testServerUrl).getStream()
    val bytesReturned: Future[Long] = futureResponse.flatMap {
      case (headers, body) =>
      // Count the number of bytes returned
      body |>>> Iteratee.fold(0l) { (total, bytes) =>
        total + bytes.length
      }
    }
    
    Ok("Done . Result legnth " + Await.result(bytesReturned, Duration.Inf))
  }
  
  def postStream = Action {
    val testServerUrl = "http://localhost:8080/test-rest-server/"
    
    val iteratee = Iteratee.foreach[Array[Byte]] { chunk =>
      val chunkString = new String(chunk, "UTF-8")
      println(chunkString)
    }
    
    val futureIteratee = WS.url(testServerUrl).postAndRetrieveStream(""){ _ => iteratee}
//    val bytesReturned: Future[Long] = futureResponse.flatMap {
//      case (headers, body) =>
//      // Count the number of bytes returned
//      body |>>> Iteratee.fold(0l) { (total, bytes) =>
//        total + bytes.length
//      }
//    }
    
    Ok("Done . " + Await.result(futureIteratee, Duration.Inf))
  }
  
}