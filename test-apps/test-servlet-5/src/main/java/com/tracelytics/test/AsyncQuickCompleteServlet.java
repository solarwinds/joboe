package com.tracelytics.test;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;


@WebServlet(urlPatterns={"/async-quick-complete-servlet"}, asyncSupported=true)
public class AsyncQuickCompleteServlet extends HttpServlet {
    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp)
        throws ServletException, IOException {
        req.startAsync();
        resp.setStatus(200);
        resp.getOutputStream().print("Hi from " + getClass().getSimpleName());
        
        req.getAsyncContext().start(new Runnable() {
            @Override
            public void run() {
                req.getAsyncContext().complete(); //completes right the way, this should NOT trigger layer exit as the main servlet is still running
            }
        });
        
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } //but the initiating thread is still running
    }
}