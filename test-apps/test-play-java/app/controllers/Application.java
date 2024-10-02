package controllers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import play.libs.F.Callback;
import play.libs.F.Callback0;
import play.libs.F.Function;
import play.libs.F.Function0;
import play.libs.F.Promise;
import play.libs.WS;
import play.libs.WS.Response;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.WebSocket;
import views.html.index;
import views.html.post;


public class Application extends Controller {

    public static Result index() {
        return ok(index.render("Your new application is ready."));
    }
    
    public static Result file() {
        return ok(new File("public/images/fish.jpg"));
    }
    
    public static Result chunkedFile() {
        try {
            return ok(new FileInputStream("public/images/fish.jpg"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return notFound();
        }
    }

    public static Result exception() {
        throw new RuntimeException("Testing exception");
    }
    
    public static Result doWait(int length) {
        try {
            Thread.sleep(length);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return ok(index.render("Waited for " + length + "(ms)")); 
    }
    
    public static Promise<Result> async() {
        Promise<Integer> promiseOfInt = Promise.promise(
                                               new Function0<Integer>() {
                                                   public Integer apply() {
                                                       try {
                                                           Thread.sleep(5000);
                                                       } catch (InterruptedException e) {
                                                           // TODO Auto-generated catch block
                                                           e.printStackTrace();
                                                       }
                                                       return 32;
                                                   }
                                               }
                                               );

        return promiseOfInt.map(FutureMapper.<Integer>getMapper());
    }
    
    public static WebSocket<String> webSocket() {
        return new WebSocket<String>() {
            
          // Called when the Websocket Handshake is done.
          public void onReady(WebSocket.In<String> in, WebSocket.Out<String> out) {
            
            // For each event received on the socket,
            in.onMessage(new Callback<String>() {
               public void invoke(String event) {
                 // Log events to the console
                 System.out.println(event);  
               } 
            });
            
            // When the socket is closed.
            in.onClose(new Callback0() {
               public void invoke() {
                   System.out.println("Disconnected");
               }
            });
            
            // Send a single 'Hello!' message
            out.write("Hello!");
            
          }
      };
    }
    
    public static Result post() {
        if ("POST".equalsIgnoreCase(request().method())) { 
            String[] values = request().body().asFormUrlEncoded().get("test");
            return ok(post.render(values != null && values.length == 1 ? "Recieved " + values[0] : "Please input value and press enter"));
        } else {
            return ok(post.render("Please input value and press enter"));
        }
    }
    
    public static Result getDirections() {
        String driveUrl = "https://maps.googleapis.com/maps/api/directions/json?origin=Vancouver&destination=Burnaby&mode=driving&key=AIzaSyCbvKeD8gVClwly9WuGh7KmcD3CEmm4wGc";
        String bikeUrl = "https://maps.googleapis.com/maps/api/directions/json?origin=Vancouver&destination=Burnaby&mode=bicycling&key=AIzaSyCbvKeD8gVClwly9WuGh7KmcD3CEmm4wGc";
        String transitUrl = "https://maps.googleapis.com/maps/api/directions/json?origin=Vancouver&destination=Burnaby&mode=transit&key=AIzaSyCbvKeD8gVClwly9WuGh7KmcD3CEmm4wGc";
                
        String[] urls = new String[] {driveUrl, bikeUrl, transitUrl};
        
        List<Promise<Response>> responsePromises = new ArrayList<Promise<Response>>();
        for (String url : urls) {
            responsePromises.add(WS.url(url).get());  
        }
        
        for (Promise<Response> responsePromise : responsePromises) {
            responsePromise.get(5000);
        }
        return ok("finished calling google");
    }
    
    public static Result crossServer() {
        String testServerUrl = "http://localhost:8080/test-rest-server/";
                
        WS.url(testServerUrl).get().get(5000);
        return ok("Done");
    }
    
    public static Promise<Result> getStream() {
        String testServerUrl = "http://localhost:8080/test-rest-server/";
        
        Promise<Result> resultPromise = WS.url(testServerUrl).get().map(
                new Function<Response, Result>() {
                    public Result apply(Response response) {
                        return ok("Response:" + response);
                    }
                } 
        );
        return resultPromise;
    }
    
    public static Promise<Result> postStream() {
        String testServerUrl = "http://localhost:8080/test-rest-server/";
        
        Promise<Result> resultPromise = WS.url(testServerUrl).post("").map(
                new Function<Response, Result>() {
                    public Result apply(Response response) {
                        return ok("Response:" + response);
                    }
                } 
        );
        return resultPromise;
    }
}
