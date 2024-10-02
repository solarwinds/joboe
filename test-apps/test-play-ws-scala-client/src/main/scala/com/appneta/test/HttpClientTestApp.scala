package com.appneta.test

import java.io.File
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.Duration
import org.asynchttpclient.AsyncHttpClientConfig
import com.appoptics.api.ext.Trace
import com.typesafe.config.ConfigFactory
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import akka.util.ByteString
import play.api.Configuration
import play.api.Environment
import play.api.Mode
import play.api.libs.ws.StreamedResponse
import play.api.libs.ws.WSClient
import play.api.libs.ws.WSConfigParser
import play.api.libs.ws.WSResponse
import play.api.libs.ws.ahc.AhcConfigBuilder
import play.api.libs.ws.ahc.AhcWSClient
import play.api.libs.ws.ahc.AhcWSClientConfig
import play.api.libs.ws.WSResponseHeaders
import java.util.logging.Level
import java.util.logging.Logger



trait HttpClientTestApp {
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  def main(args: Array[String]) {
    Logger.getLogger("io.netty").setLevel(Level.OFF);  
    
    val configuration = Configuration.reference ++ Configuration(ConfigFactory.parseString(
      """
    |ws.followRedirects = true
  """.stripMargin))

    // If running in Play, environment should be injected
    val environment = Environment(new File("."), this.getClass.getClassLoader, Mode.Prod)

    val parser = new WSConfigParser(configuration, environment)
    val config = new AhcWSClientConfig(wsClientConfig = parser.parse())
    val builder = new AhcConfigBuilder(config)
//    val logging = new AsyncHttpClientConfig.AdditionalChannelInitializer() {
//      override def initChannel(channel: io.netty.channel.Channel): Unit = {
//        channel.pipeline.addFirst("log", new io.netty.handler.logging.LoggingHandler("warning"))
//      }
//    }
    val ahcBuilder = builder.configure()
    
//    ahcBuilder.setHttpAdditionalChannelInitializer(logging)
    val ahcConfig = ahcBuilder.build()
    val wsClient = new AhcWSClient(ahcConfig)
    
    
    val layerName = "test-ws-" + getClass.getSimpleName
    try {
      Trace.startTrace(layerName).report()
      
      val url = if (args.length == 0) { "http://localhost:8080/test-rest-server" } else { args(0) }
      val future = test(wsClient, url)
      
      val response = Await.result(future, Duration.Inf)
      val statusCode = response match {
        case res : WSResponse => res.status
        case streamedResponse : StreamedResponse => 
          val bytesReturnedFuture: Future[Long] = 
              // Count the number of bytes returned
              streamedResponse.body.runWith(Sink.fold[Long, ByteString](0L) { (total, bytes) =>
                total + bytes.length
              })
          bytesReturnedFuture.foreach { bytesReturned => println(s"Bytes returned: $bytesReturned") }    
          
          streamedResponse.headers.status
        case _ =>
          -1
      }
       
      println(s"Response: $response")
      println(s"Status: $statusCode")
      
      
    } finally {
      Trace.endTrace(layerName)
      wsClient.close()
      
      Await.result(system.terminate(), Duration.Inf)
      println("System terminated!!!")
    }

  }

  def test(wsClient: WSClient, args : String): Future[Any]
}