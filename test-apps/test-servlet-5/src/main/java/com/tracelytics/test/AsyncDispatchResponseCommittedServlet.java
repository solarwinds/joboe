package com.tracelytics.test;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;


@WebServlet(urlPatterns={"/async-dispatch-response-committed-servlet"}, asyncSupported=true)
public class AsyncDispatchResponseCommittedServlet extends HttpServlet {
    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp)
        throws ServletException, IOException {
        req.startAsync();
        resp.setStatus(200);
        resp.getOutputStream().print("Hi from " + getClass().getSimpleName());
        resp.getOutputStream().flush();
        resp.getOutputStream().close();
        
        req.getAsyncContext().dispatch("/test-servlet");
    }
}
