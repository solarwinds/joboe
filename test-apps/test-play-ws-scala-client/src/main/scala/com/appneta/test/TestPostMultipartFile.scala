package com.appneta.test

import java.io.File
import java.io.PrintWriter

import scala.util.Random

import akka.stream.scaladsl.FileIO
import akka.stream.scaladsl.Source
import play.api.libs.ws.WSClient
import play.api.mvc.MultipartFormData.DataPart
import play.api.mvc.MultipartFormData.FilePart

object TestPostMultipartFile extends HttpClientTestApp {

  private[this] lazy val tmpFile = generateFile()

  override def test(wsClient: WSClient, url: String) = {
    val future = wsClient.url(url).post(Source(FilePart("file", "random", Option("text/plain"), FileIO.fromFile(tmpFile)) :: DataPart("key", "value") :: List()))
    
    tmpFile.delete
    
    future
  }

  private def generateFile(): File = {
    val file = File.createTempFile("test-post-multipart", null)
    val printWriter = new PrintWriter(file)
    (1 to 10000).foreach { _ =>
      printWriter.write(Random.nextString(10))
    }

    printWriter.flush()
    
    file
  }
}