package controllers;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import play.db.DB;
import play.mvc.Controller;
import play.mvc.Result;

import views.html.*;

public class Application extends Controller {
    private static final String QUERY = "SELECT 9 as testkey";
    private static final int CALL_COUNT = 3; //number of current calls
    
    public Result index() {
        return ok(index.render("Your new application is ready."));
    }
    
    public CompletionStage<Result> async() throws SQLException {
        List<CompletableFuture<String>> promises = new ArrayList<CompletableFuture<String>>();
        
        for (int i = 0 ; i < CALL_COUNT; i ++) {
            promises.add(getJdbcPromise());
            
        }
        
        //return promises.stream().map( future -> future.get()).reduce("", (merged, element) -> merged + element);
        return promises.stream().reduce(CompletableFuture.completedFuture(""), (merged, element) -> merged.thenCombineAsync(element, (mergedString, elementString) -> mergedString + elementString)).thenApply( resultString -> ok(resultString));
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
    
    private static CompletableFuture<String> getJdbcPromise() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return executeJdbc();
            } catch (SQLException e) {
                return null;
            }
        });
    }
}
