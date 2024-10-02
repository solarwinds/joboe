package com.appneta.test

import java.io.File

import akka.stream.scaladsl.Source
import akka.util.ByteString
import play.api.libs.ws.StreamedBody
import play.api.libs.ws.WSClient

object TestPutStream extends HttpClientTestApp {
  override def test(wsClient : WSClient, url: String) = {
    val tmpFile = new File("C:/Users/Public/Music/Sample Music/Kalimba.mp3")
    //val future = wsClient.url(url).post(Source(FilePart("song", "song.mp3", Option("audio/mpeg"), FileIO.fromFile(tmpFile)) :: DataPart("key", "value") :: List()))

    val byteString = ByteString.fromString("lolololololol")

    val streamedBody = StreamedBody(Source.unfold(0) { i => if (i < 100) { Thread.sleep(100); Some(i + 1, byteString) } else { None } })

    val future = wsClient.url(url).withBody(streamedBody).execute("PUT")

    future
  }
  
}