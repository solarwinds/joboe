package com.tracelytics.instrumentation.http;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class HttpServletStub extends HttpServlet {
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException {
        //do nothing
    }
    
    @Override
    public void service(ServletRequest req, ServletResponse res)
        throws ServletException, IOException {
        //do nothing
    }
}
