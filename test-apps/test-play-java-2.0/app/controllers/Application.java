package controllers;

import java.util.ArrayList;
import java.util.List;

import play.*;
import play.libs.WS;
import play.libs.F.Promise;
import play.libs.WS.Response;
import play.mvc.*;
import views.html.*;

public class Application extends Controller {
  
  public static Result index() {
    return ok(index.render("Your new application is ready."));
  }
  
  public static Result getDirections() {
      String driveUrl = "https://maps.googleapis.com/maps/api/directions/json?origin=Vancouver&destination=Burnaby&mode=driving&key=AIzaSyCbvKeD8gVClwly9WuGh7KmcD3CEmm4wGc";
      String bikeUrl = "https://maps.googleapis.com/maps/api/directions/json?origin=Vancouver&destination=Burnaby&mode=bicycling&key=AIzaSyCbvKeD8gVClwly9WuGh7KmcD3CEmm4wGc";
      String transitUrl = "https://maps.googleapis.com/maps/api/directions/json?origin=Vancouver&destination=Burnaby&mode=transit&key=AIzaSyCbvKeD8gVClwly9WuGh7KmcD3CEmm4wGc";
              
      String[] urls = new String[] {driveUrl, bikeUrl, transitUrl};
      
      List<Promise<Response>> responsePromises = new ArrayList<Promise<Response>>();
      for (String url : urls) {
          Promise<Response> promise = WS.url(url).get();
//          promise.map(new Function<WS.Response, WS.Response>() {
//              @Override
//              public Response apply(Response a) throws Throwable {
//                  System.out.println("something!");
//                  return a;
//              }
//          }).recover(new Function<Throwable, WS.Response>() {
//
//              @Override
//              public Response apply(Throwable a) throws Throwable {
//                  System.out.println("exception something");
//                  throw a;
//              }
//          });
          responsePromises.add(promise);  
      }
      
      for (Promise<Response> responsePromise : responsePromises) {
          responsePromise.get();
      }
      try {
          Thread.sleep(1000);
      } catch (InterruptedException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
      }
      
      return ok("finished calling google");
  }
  
  public static Result crossServer() {
      String testServerUrl = "http://localhost:8080/test-rest-server/";
              
      Promise<Response> promise = WS.url(testServerUrl).get();
      promise.get();
      return ok("Done");
  }
}