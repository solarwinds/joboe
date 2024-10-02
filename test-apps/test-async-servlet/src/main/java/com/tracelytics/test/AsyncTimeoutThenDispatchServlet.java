package com.tracelytics.test;

import java.io.IOException;

import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


@WebServlet(urlPatterns={"/async-timeout-then-dispatch-servlet"}, asyncSupported=true)
public class AsyncTimeoutThenDispatchServlet extends HttpServlet {
    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp)
        throws ServletException, IOException {
        req.startAsync();
        resp.setStatus(200);
        resp.getOutputStream().print("Hi from " + getClass().getSimpleName());
        
        req.getAsyncContext().setTimeout(15000);
        req.getAsyncContext().addListener(new AsyncListener() {
            
            @Override
            public void onTimeout(AsyncEvent event) throws IOException {
                event.getAsyncContext().dispatch("/test-servlet");
            }
            
            @Override
            public void onStartAsync(AsyncEvent event) throws IOException {
                // TODO Auto-generated method stub
                
            }
            
            @Override
            public void onError(AsyncEvent event) throws IOException {
                // TODO Auto-generated method stub
                
            }
            
            @Override
            public void onComplete(AsyncEvent event) throws IOException {
                // TODO Auto-generated method stub
                
            }
        });
        
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
