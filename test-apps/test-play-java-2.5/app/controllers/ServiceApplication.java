package controllers;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;

import play.http.HttpEntity;
import play.libs.ws.StreamedResponse;
import play.libs.ws.WSClient;
import play.libs.ws.WSResponse;
import play.libs.ws.WSResponseHeaders;
import play.mvc.Controller;
import play.mvc.Http.MultipartFormData.DataPart;
import play.mvc.Http.MultipartFormData.FilePart;
import play.mvc.Http.Status;
import play.mvc.Result;
import akka.stream.Materializer;
import akka.stream.javadsl.FileIO;
import akka.stream.javadsl.Source;
import akka.util.ByteString;

/**
 * Application that sends request to another app server. It tests the Play-WS java client instrumentation
 * @author pluk
 *
 */
public class ServiceApplication extends Controller {
    private static final String REST_SERVER_URL = "http://localhost:8080/test-rest-server/";
    
    @Inject WSClient ws;
    @Inject Materializer materializer;
    
    public Result getDirections() throws InterruptedException, ExecutionException {
        String driveUrl = "https://maps.googleapis.com/maps/api/directions/json?origin=Vancouver&destination=Burnaby&mode=driving&key=AIzaSyCbvKeD8gVClwly9WuGh7KmcD3CEmm4wGc";
        String bikeUrl = "https://maps.googleapis.com/maps/api/directions/json?origin=Vancouver&destination=Burnaby&mode=bicycling&key=AIzaSyCbvKeD8gVClwly9WuGh7KmcD3CEmm4wGc";
        String transitUrl = "https://maps.googleapis.com/maps/api/directions/json?origin=Vancouver&destination=Burnaby&mode=transit&key=AIzaSyCbvKeD8gVClwly9WuGh7KmcD3CEmm4wGc";
                
        String[] urls = new String[] {driveUrl, bikeUrl, transitUrl};
        
        List<CompletionStage<WSResponse>> responsePromises = new ArrayList<CompletionStage<WSResponse>>();
        for (String url : urls) {
            responsePromises.add(ws.url(url).get());  
        }
        
        for (CompletionStage<WSResponse> responsePromise : responsePromises) {
            responsePromise.toCompletableFuture().get();
        }
        return ok("finished calling google");
    }
    
    public Result crossServer() throws InterruptedException, ExecutionException {
        ws.url(REST_SERVER_URL).get().toCompletableFuture().get();
        return ok("Done");
    }
    
    public CompletionStage<Result> getSimple() {
        CompletionStage<Result> resultPromise = ws.url(REST_SERVER_URL).get().thenApply( response -> {
                return ok("Response:" + response);
            });
        return resultPromise;
    }
    
    public CompletionStage<Result> getStream() {
     // Make the request
        CompletionStage<StreamedResponse> futureResponse = ws.url(REST_SERVER_URL + "?stream=true").setMethod("GET").stream();

        CompletionStage<Result> result = futureResponse.thenApply(response -> {
            WSResponseHeaders responseHeaders = response.getHeaders();
            Source<ByteString, ?> body = response.getBody();
            // Check that the response was successful
            if (responseHeaders.getStatus() == 200) {
                // Get the content type
                String contentType =
                        Optional.ofNullable(responseHeaders.getHeaders().get("Content-Type"))
                                .map(contentTypes -> contentTypes.get(0))
                                .orElse("application/octet-stream");

                // If there's a content length, send that, otherwise return the body chunked
                Optional<String> contentLength = Optional.ofNullable(responseHeaders.getHeaders()
                        .get("Content-Length"))
                        .map(contentLengths -> contentLengths.get(0));
                if (contentLength.isPresent()) {
                    return ok().sendEntity(new HttpEntity.Streamed(
                            body,
                            Optional.of(Long.parseLong(contentLength.get())),
                            Optional.of(contentType)
                    ));
                } else {
                    return ok().chunked(body).as(contentType);
                }
            } else {
                return new Result(Status.BAD_GATEWAY);
            }
        });
        
        return result;
    }
    
    
    
    public CompletionStage<Result> postMultipart() throws IOException {
        Source<ByteString, ?> file = FileIO.fromFile(generateFile());
        FilePart<Source<ByteString, ?>> filePart = new FilePart<>("hello", "hello.txt", "text/plain", file);
        DataPart dataPart = new DataPart("testKey", "xyz");
        
        CompletionStage<Result> resultPromise = ws.url(REST_SERVER_URL).post(Source.from(Arrays.asList(filePart, dataPart))).thenApplyAsync( response -> {
            return ok("Response:" + response);
        });
        
        return resultPromise;
    }
    
    public CompletionStage<Result> putBigFile() throws IOException {
        Source<ByteString, ?> file = FileIO.fromFile(generateFile());
        
        CompletionStage<Result> resultPromise = ws.url(REST_SERVER_URL).setBody(file).execute("PUT").thenApply( response -> {
            return ok("Response:" + response);
        });
        
        return resultPromise;
    }
    
    public CompletionStage<Result> timeout() {
        return ws.url(REST_SERVER_URL + "?duration=2000").setRequestTimeout(1000).get().thenApply( response -> {
            return ok("Response:" + response);
        });
    }
 
    private static File generateFile() throws IOException {
        File file = File.createTempFile("test-file", null);
        try (PrintWriter printWriter = new PrintWriter(file)) {
            for (int i = 0 ; i < 100000; i ++) {
                printWriter.write("abcdefghijklmnopqrstuvwxyz");
            }
            printWriter.flush();
        }

        return file;
    }
    
}
