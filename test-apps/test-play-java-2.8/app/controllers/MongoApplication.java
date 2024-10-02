package controllers;

import org.bson.Document;

import com.mongodb.ReadPreference;
import com.mongodb.async.client.MongoClient;
import com.mongodb.async.client.MongoClients;
import com.mongodb.async.client.MongoCollection;
import com.mongodb.async.client.MongoDatabase;
import com.mongodb.async.client.MongoIterable;

import play.mvc.Controller;
import play.mvc.Result;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import views.html.*;

public class MongoApplication extends Controller {
    private static final String HOST_STRING = "mongodb://localhost"; //single setup
    private static final String TEST_COLLECTION = "testCollection";
    private static final String TEST_DB = "testDb";
    private static final String KEY_VALUE = "testKey";
    
    public CompletionStage<Result> index() {
        MongoClient mongoClient = MongoClients.create(HOST_STRING);
        MongoDatabase mongoDatabase = mongoClient.getDatabase(TEST_DB);
        MongoCollection<Document> collection = mongoDatabase.getCollection(TEST_COLLECTION);
        
        
        Document basicDocument = new Document("key", KEY_VALUE);
        return iterateResult(collection.find(basicDocument)).thenApply(mongoResult -> ok(index.render("Mongo async operation is executed")));
   }
    
    protected static CompletableFuture<Object> iterateResult(MongoIterable<?> iterable) {
        CompletableFuture<Object> completableFuture = new CompletableFuture<>();
        iterable.forEach(null, (Void result, Throwable t) -> completableFuture.complete(result));
        
        return completableFuture;
    }
   
}
