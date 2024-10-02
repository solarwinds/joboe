package controllers;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.inject.Inject;

import play.db.DB;
import play.libs.F.Promise;
import play.libs.ws.WSClient;
import play.mvc.Controller;
import play.mvc.Result;

import views.html.*;

public class DeprecatedApplication extends Controller {
    private static final String QUERY = "SELECT 9 as testkey";
    private static final int CALL_COUNT = 3; //number of current calls
    
    public Promise<Result> async() throws SQLException {
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
        return Promise.promise(() -> { 
            try {
                return executeJdbc();
            } catch (SQLException e) {
                return "error";
            }
        });
    }
}
