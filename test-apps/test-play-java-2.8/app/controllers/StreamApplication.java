package controllers;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import akka.japi.Pair;
import akka.stream.OverflowStrategy;
import akka.stream.javadsl.Source;
import akka.util.ByteString;
import play.http.HttpEntity.Streamed;
import play.mvc.Controller;
import play.mvc.Result;

public class StreamApplication extends Controller {
    private File tempFile = generateFile();
    private Source<ByteString, ?> source = Source.unfold(0, i -> { 
            if (i < 100) { 
                Thread.sleep(100); 
                return Optional.of(new Pair<Integer, ByteString>(i + 1, ByteString.fromString(i + " XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX"))); 
            } else { 
                return Optional.empty(); 
            } 
        });
    
            
            
    
//    public Result index() {
//        
//    }
    public Result streamFile() {
        return ok(tempFile);
    }
    
    public Result streamChunked() {
        return ok().chunked(source);
    }
    
    public Result streamEntity() {
        return ok().sendEntity(new Streamed(source, Optional.empty(), Optional.empty()));
    }
    
    private File generateFile() {
        try {
            File file = File.createTempFile("test-post-multipart.txt", null);
            try (
                 PrintWriter printWriter = new PrintWriter(file);
            ) {
            
                Random random = new Random();
                for (int i = 0 ; i < 10000; i ++) {
                    printWriter.write(random.nextInt(10));
                }
                
                printWriter.flush();
                return file;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return null;
    }
}
