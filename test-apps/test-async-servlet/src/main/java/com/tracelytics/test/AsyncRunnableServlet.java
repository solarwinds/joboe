package com.tracelytics.test;

import java.io.IOException;

import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


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
