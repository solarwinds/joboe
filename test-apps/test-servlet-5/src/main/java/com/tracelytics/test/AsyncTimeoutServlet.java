package com.tracelytics.test;

import java.io.IOException;

import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;


@WebServlet(urlPatterns={"/async-timeout-servlet"}, asyncSupported=true)
public class AsyncTimeoutServlet extends HttpServlet {
    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp)
        throws ServletException, IOException {
        req.startAsync();
        resp.setStatus(200);
        resp.getOutputStream().print("Hi from " + getClass().getSimpleName());
//        
//        req.getAsyncContext().addListener(new AsyncListener() {
//            
//            @Override
//            public void onTimeout(AsyncEvent event) throws IOException {
//                event.getAsyncContext().complete();
//            }
//            
//            @Override
//            public void onStartAsync(AsyncEvent event) throws IOException {
//                // TODO Auto-generated method stub
//                
//            }
//            
//            @Override
//            public void onError(AsyncEvent event) throws IOException {
//                // TODO Auto-generated method stub
//                
//            }
//            
//            @Override
//            public void onComplete(AsyncEvent event) throws IOException {
//                // TODO Auto-generated method stub
//                
//            }
//        });
        
        req.getAsyncContext().setTimeout(15000);
        
        req.getAsyncContext().start(new Runnable() {

            @Override
            public void run() {
                try {
                    Thread.sleep(10000000);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } //sleep for a long time to trigger timeout
                req.getAsyncContext().complete(); 
            }
        });
        
        
    }
}
