package controllers

import play.api._
import play.api.mvc._
import play.api.libs.ws._
import play.api.Play.current
import play.api.db._
import java.net.URL
import java.io.InputStream
import play.libs.F.Function0
import java.util.concurrent.TimeUnit
import java.lang.RuntimeException
import play.api.libs.iteratee.Iteratee

object ServiceApplication extends Controller {
  def getDirections = Action {
    val driveUrl = "https://maps.googleapis.com/maps/api/directions/json?origin=Vancouver&destination=Burnaby&mode=driving&key=AIzaSyCbvKeD8gVClwly9WuGh7KmcD3CEmm4wGc"
    val bikeUrl = "https://maps.googleapis.com/maps/api/directions/json?origin=Vancouver&destination=Burnaby&mode=bicycling&key=AIzaSyCbvKeD8gVClwly9WuGh7KmcD3CEmm4wGc"
    val transitUrl = "https://maps.googleapis.com/maps/api/directions/json?origin=Vancouver&destination=Burnaby&mode=transit&key=AIzaSyCbvKeD8gVClwly9WuGh7KmcD3CEmm4wGc"
    val urls = Seq(driveUrl, bikeUrl, transitUrl)
    val responses = urls.map { WS.url(_).get }
    //val results = Future.sequence(responses).get(5000)
    
//    results.foreach { result => println(result.json) }
    
//    val durations = results.map {
//      result => (((result.json \ "routes")(0) \ "legs")(0) \ "duration" \ "value").as[Int]
//    }
      Ok("FINISHED")   
//    Ok("finised calling google" + durations.foldLeft("")( (foldString, duration) => foldString + duration + " | "))
  }
  
  def crossServer = Action { request =>
    val testServerUrl = "http://localhost:8080/test-rest-server/"
    
    WS.url(testServerUrl).get().value
    Ok("Done")
  }
  
  def getStream = Action {
    val testServerUrl = "http://localhost:8080/test-rest-server/"
    //val testServerUrl   = "https://maps.googleapis.com/maps/api/directions/json?origin=Vancouver&destination=Burnaby&mode=driving&key=AIzaSyCbvKeD8gVClwly9WuGh7KmcD3CEmm4wGc"
    
    val iteratee = Iteratee.foreach[Array[Byte]] { chunk =>
      val chunkString = new String(chunk, "UTF-8")
      println(chunkString)
    }
//    val newIteratee = iteratee.compose { headers : ResponseHeaders => 
//      println(headers) 
//      headers
//    }
    
    val futureIteratee = WS.url(testServerUrl).get(_ => iteratee)
    println("Iteratee is " + futureIteratee) //why is that null? Play WS bug?
    Ok("Done . Result legnth ")
  }
  
  def postStream = Action {
    val testServerUrl = "http://localhost:8080/test-rest-server/"
    //val testServerUrl   = "https://maps.googleapis.com/maps/api/directions/json?origin=Vancouver&destination=Burnaby&mode=driving&key=AIzaSyCbvKeD8gVClwly9WuGh7KmcD3CEmm4wGc"
    
    val iteratee = Iteratee.foreach[Array[Byte]] { chunk =>
      val chunkString = new String(chunk, "UTF-8")
      println(chunkString)
    }
//    val newIteratee = iteratee.compose { headers : ResponseHeaders => 
//      println(headers) 
//      headers
//    }
    
    val futureIteratee = WS.url(testServerUrl).postAndRetrieveStream("")(_ => iteratee)
    println("Iteratee is " + futureIteratee) //why is that null? Play WS bug?
    Ok("Done . Result legnth ")
  }
}