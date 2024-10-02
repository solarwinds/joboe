package com.tracelytics.test;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


public class TestServlet extends HttpServlet {
    private static final Log log = LogFactory.getLog(TestServlet.class);
    private static final Logger jbossLogger = Logger.getLogger(TestServlet.class);

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException {
        log.info("Apache common logger");
        jbossLogger.info("jboss logger");
        resp.getOutputStream().write("Hello!".getBytes());
    }
}
