package com.tracelytics.test;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.tracelytics.test.sprawler.Sprawler;
import com.tracelytics.test.sprawler.SprawlerManager;

public class SprawlerServlet extends HttpServlet {
    private static final int DEFAULT_DEPTH = 2;
    private static final int DEFAULT_MAX_DEPTH = 5;
    private static final long DEFAULT_SLEEP = 0;
    
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException {
        
        int depth;
        
        try {
            depth = Integer.parseInt(req.getParameter("depth"));
        } catch (NumberFormatException e) {
            depth = DEFAULT_DEPTH;
        }        
        
        long sleep = DEFAULT_SLEEP; //need a sleep such that trace end event does not fire before the rest of the asynchronous events do. This should not be necessary if 
        //https://github.com/tracelytics/tracelons/issues/3352 is addressed
        if (req.getParameter("sleep") != null) {
            try {
                sleep = Long.parseLong(req.getParameter("sleep"));
            } catch (NumberFormatException e) {
                sleep = DEFAULT_SLEEP;
            }   
        }
        
        
        
        if (depth < 0 || depth > DEFAULT_MAX_DEPTH) {
            depth = DEFAULT_DEPTH;
        }
        
        String startingUrl = req.getParameter("startingUrl");

        String solrServerUrl = req.getParameter("solrServerUrl");

        if (startingUrl != null) {
            final Sprawler sprawler = SprawlerManager.buildSprawler(startingUrl, solrServerUrl , depth);
            new Thread () { 
                @Override
                public void run() {
                    sprawler.collect();
                }
            }.start();
            
         
            try {
                Thread.sleep(sleep);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            
            req.setAttribute("sprawlerId", sprawler.getId());
            req.getRequestDispatcher("sprawler.jsp").forward(req, resp);
        } else {
            req.getRequestDispatcher("sprawler.jsp").forward(req, resp);
        }

        
        
        
        
    }
    
    
}
