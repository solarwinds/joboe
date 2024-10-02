package controllers;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import play.*;
import play.db.DB;
import play.libs.F.Function;
import play.libs.F.Promise;
import play.libs.ws.WS;
import play.libs.ws.WSClient;
import play.libs.ws.WSResponse;
import play.mvc.*;
import views.html.*;

public class Application extends Controller {
    private static final String QUERY = "SELECT 9 as testkey";
    private static final int CALL_COUNT = 3; //number of current calls
    
    @Inject WSClient ws;
    public Result index() {
        return ok(index.render("Your new application is ready."));
    }
    
    public Promise<Result> async() {
        Promise<String>[] promises = new Promise[CALL_COUNT];
        
        for (int i = 0 ; i < CALL_COUNT; i ++) {
            promises[i] = getJdbcPromise();
        }
        
        //return Promise.sequence(promises).map(jdbcResults -> ok(jdbcResults.stream().reduce("", (t, u) -> t + " " + u)));
        return Promise.sequence(promises).map(jdbcResults -> ok(index.render(jdbcResults.stream().reduce("", 
                (t, u) -> {
                    return t + " " + u ;
                }
                ))));
    }
    
    public Result getDirections() {
        String driveUrl = "https://maps.googleapis.com/maps/api/directions/json?origin=Vancouver&destination=Burnaby&mode=driving&key=AIzaSyCbvKeD8gVClwly9WuGh7KmcD3CEmm4wGc";
        String bikeUrl = "https://maps.googleapis.com/maps/api/directions/json?origin=Vancouver&destination=Burnaby&mode=bicycling&key=AIzaSyCbvKeD8gVClwly9WuGh7KmcD3CEmm4wGc";
        String transitUrl = "https://maps.googleapis.com/maps/api/directions/json?origin=Vancouver&destination=Burnaby&mode=transit&key=AIzaSyCbvKeD8gVClwly9WuGh7KmcD3CEmm4wGc";
                
        String[] urls = new String[] {driveUrl, bikeUrl, transitUrl};
        
        List<Promise<WSResponse>> responsePromises = new ArrayList<Promise<WSResponse>>();
        for (String url : urls) {
            responsePromises.add(ws.url(url).get());  
        }
        
        for (Promise<WSResponse> responsePromise : responsePromises) {
            responsePromise.get(5000);
        }
        return ok("finished calling google");
    }
    
    public Result crossServer() {
        String testServerUrl = "http://localhost:8080/test-rest-server/";
                
        ws.url(testServerUrl).get().get(5000);
        return ok("Done");
    }
    
    public Promise<Result> getStream() {
        String testServerUrl = "http://localhost:8080/test-rest-server/";
        
        Promise<Result> resultPromise = WS.url(testServerUrl).get().map( response -> {
                return ok("Response:" + response);
            });
        return resultPromise;
    }
    
    public Promise<Result> postStream() {
        String testServerUrl = "http://localhost:8080/test-rest-server/";
        
        
        Promise<Result> resultPromise = WS.url(testServerUrl).post("").map( response -> {
            return ok("Response:" + response);
        });
        
        return resultPromise;
    }
    
    private static String executeJdbc() throws SQLException {
        String outString = "Number is ";
        try (Connection connection = DB.getConnection()) {
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(QUERY);
            
            while (resultSet.next()) {
                outString += resultSet.getString("testkey");            
            }
        }
        
        
        return outString;
    }
    
    private static Promise<String> getJdbcPromise() {
        return Promise.promise(() -> executeJdbc());
    }
}
