package com.tracelytics.test;

import java.io.IOException;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;


@WebServlet(urlPatterns={"/async-runnable-servlet"}, asyncSupported=true)
public class AsyncRunnableServlet extends HttpServlet {
    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp)
        throws ServletException, IOException {
        AsyncContext asyncContext= req.startAsync();
        resp.setStatus(200);
        resp.getOutputStream().print("Hi from " + getClass().getSimpleName());
        
        
        asyncContext.start(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000); //sleep for 1 secs then complete the request
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    req.getAsyncContext().complete();
                }
            }
        });
    }
}
