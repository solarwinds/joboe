package controllers

import play.api._
import play.api.mvc._
import play.api.libs.ws._
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
import play.api.libs.ws.ning.NingWSRequest
import play.api.libs.ws.ning.NingWSClient
import com.ning.http.client.RequestBuilder
import play.api.libs.iteratee.Iteratee

object ServiceApplication extends Controller {
  def getLink = Action {
    Ok("<form action='get-directions' method='POST'><input type='submit' value='Submit'></form>").as("text/html")
  }
  
  def getDirections = Action {
    val driveUrl = "https://maps.googleapis.com/maps/api/directions/json?origin=Vancouver&destination=Burnaby&mode=driving&key=AIzaSyCbvKeD8gVClwly9WuGh7KmcD3CEmm4wGc"
    val bikeUrl = "https://maps.googleapis.com/maps/api/directions/json?origin=Vancouver&destination=Burnaby&mode=bicycling&key=AIzaSyCbvKeD8gVClwly9WuGh7KmcD3CEmm4wGc"
    val transitUrl = "https://maps.googleapis.com/maps/api/directions/json?origin=Vancouver&destination=Burnaby&mode=transit&key=AIzaSyCbvKeD8gVClwly9WuGh7KmcD3CEmm4wGc"
    
    val urls = Seq(driveUrl, bikeUrl, transitUrl)
    val responses = urls.map { WS.url(_).get }
    val results = Await.result(Future.sequence(responses), Duration.Inf)
    
//    results.foreach { result => println(result.json) }
    
    val durations = results.map {
      result => (((result.json \ "routes")(0) \ "legs")(0) \ "duration" \ "value").as[Int]
    }
    
    Ok("finised calling google" + durations.foldLeft("")( (foldString, duration) => foldString + duration + " | "))
    
  }
  
  def crossServer = Action {
    val testServerUrl = "http://localhost:8080/test-rest-server/"
    
    Await.result(WS.url(testServerUrl).get(), Duration.Inf)
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