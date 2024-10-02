package com.tracelytics.test;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

@WebServlet(urlPatterns={"/test-forward-servlet"}, asyncSupported=true)
public class TestForwardServlet extends HttpServlet {
    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp)
        throws ServletException, IOException {
        try {
              Thread.sleep(1000); //sleep for 1 secs then complete the request
        } catch (Exception e) {
            e.printStackTrace();
        }
        resp.setStatus(200);
        resp.getOutputStream().print("Hi from " + getClass().getSimpleName());
        
        getServletContext().getRequestDispatcher("/test-servlet").forward(new HttpServletRequestWrapper(req), new HttpServletResponseWrapper(resp));
    }
}
