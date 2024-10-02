package controllers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import play.mvc.Controller;
import play.mvc.results.Result;

public class Application extends Controller {

    public static void index() {
        render();
    }

    public static void file() {
        renderBinary(new File("public/images/fish.jpg"));
    }
    
    public static void stream() {
        try {
            renderBinary(new FileInputStream("public/images/fish.jpg"));
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static void exception() {
        throw new RuntimeException("Testing exception");
    }
    
    public static void doWait() {
        try {
            Integer length = request.params.get("length", Integer.class);
            
            Thread.sleep(length);
            renderArgs.put("length", length);
            render("wait.html"); 
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
    }
    
    public static void post() {
       render("post.html");
    }
}