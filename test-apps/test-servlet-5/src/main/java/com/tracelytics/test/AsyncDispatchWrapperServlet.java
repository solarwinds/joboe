package com.tracelytics.test;

import java.io.IOException;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;


@WebServlet(urlPatterns={"/async-dispatch-wrapper-servlet"}, asyncSupported=true)
public class AsyncDispatchWrapperServlet extends HttpServlet {
    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp)
        throws ServletException, IOException {
        AsyncContext asyncContext = req.startAsync(new HttpServletRequestWrapper(req), new HttpServletResponseWrapper(resp));
        resp.setStatus(200);
        resp.getOutputStream().print("Hi from " + getClass().getSimpleName());
        
        asyncContext.start(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                asyncContext.dispatch("/test-servlet");
            }
            
        });
        
    }
}
