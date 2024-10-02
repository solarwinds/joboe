package controllers

import java.io.File
import java.io.PrintWriter
import scala.util.Random
import scala.concurrent.ExecutionContext.Implicits.global
import akka.stream.scaladsl.Source
import akka.util.ByteString
import play.api.http.HttpEntity.Streamed
import play.api.mvc.Action
import play.api.http.FileMimeTypes
import play.api.mvc.BaseController
import javax.inject.Inject
import play.api.mvc.ControllerComponents
import play.api.mvc.AbstractController


class StreamApplication @Inject()(cc: ControllerComponents) extends AbstractController(cc) {
  val tempFile = generateFile()
  val stringChunk = ByteString.fromString("lolololololol")
  val chunkSource = Source.unfold(0) { i => if (i < 100) { Thread.sleep(100); Some(i + 1, stringChunk) } else { None } }
  
  def streamFile = Action {
    Ok.sendFile(tempFile)
  }
  
  def streamEntity = Action {
    Ok.sendEntity(Streamed(chunkSource, None, None))
  }
  
  def streamChunked = Action {
    Ok.chunked(chunkSource)
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