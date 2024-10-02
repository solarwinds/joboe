package controllers

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.Duration
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import javax.inject.Inject
import play.api.libs.json.JsLookupResult.jsLookupResultToJsLookup
import play.api.libs.json.JsValue.jsValueToJsLookup
import play.api.libs.ws.WSClient
import play.api.mvc.Action
import play.api.libs.ws.WSResponse
import play.api.mvc.ControllerComponents
import play.api.mvc.AbstractController

class ServiceApplication @Inject() (ws: WSClient, cc: ControllerComponents)(implicit ec: ExecutionContext) extends AbstractController(cc) {
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  
  def getLink = Action {
    Ok("<form action='get-directions' method='POST'><input type='submit' value='Submit'></form>").as("text/html")
  }
  
  def getDirections = Action {
    val driveUrl = "http://www.google.com/search?q=drive"
    val bikeUrl = "http://www.google.com/search?q=bike"
    val transitUrl = "http://www.google.com/search?q=transit"

//    AIzaSyBeFsSfQ8u-MQvdNkNMI4JtSXcabY5ReSk
//    AIzaSyBeFsSfQ8u-MQvdNkNMI4JtSXcabY5ReSk
    val urls = Seq(driveUrl, bikeUrl, transitUrl)
    val responses = urls.map { ws.url(_).get }
    val results = Await.result(Future.sequence(responses), Duration.Inf)
    
//    results.foreach { result => println(result.json) }
    
//    val durations = results.map {
//      result => (((result.json \ "routes")(0) \ "legs")(0) \ "duration" \ "value").as[Int]
//    }
//
//    Ok("finised calling google" + durations.foldLeft("")( (foldString, duration) => foldString + duration + " | "))
    Ok("finised calling google")
  }
  
  def crossServer = Action {
    val testServerUrl = "http://localhost:8080/test-rest-server/"
    
    Await.result(ws.url(testServerUrl).get(), Duration.Inf) //use default client
    Ok("Done")
  }
  

  def getStream = Action {
    val testServerUrl = "http://localhost:8080/test-rest-server/"
    
    val futureResponse = ws.url(testServerUrl).stream()
    
    val bytesReturned: Future[Long] = futureResponse.flatMap { streamedResponse =>
      streamedResponse.bodyAsSource.runFold(0l) { (total, bytes) => total + bytes.length }
    }
    
    Ok("Done . Result legnth " + Await.result(bytesReturned, Duration.Inf))
  }
  
 
  def postStream = Action {
    val testServerUrl = "http://localhost:8080/test-rest-server/"
    
    val futureResponse : Future[WSResponse] = ws.url(testServerUrl).withMethod("POST").withBody("").stream()
    Ok("Done . " + Await.result(futureResponse, Duration.Inf))
  }
  
}