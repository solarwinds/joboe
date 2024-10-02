package com.appoptics.test;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Random;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * A simple rest servlet that serves RESTFul request, it simply echo back the uri and the http method
 * 
 * Accepts all 4 operations of REST - GET, PUT, POST, DELETE. If a duration parameter is specified at the end of the uri, then the process will wait based on the numeric value in milli second
 * 
 * @author Patson Luk
 *
 */
public class RestServlet extends HttpServlet {
    private static Logger logger = LogManager.getLogger(RestServlet.class);
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException {
        if (req.getMethod().equals("PATCH")) {
            doPatch(req, resp);
        } else {
            super.service(req, resp);
        }
    }

    private void doPatch(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        handleRequest(req, resp);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException {
        handleRequest(req, resp);
    }
    
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException {
        handleRequest(req, resp);
    }
    
    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException {
        handleRequest(req, resp);
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException {
        handleRequest(req, resp);
    }
    
    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException {
        handleRequest(req, resp);
    }
    
    @Override
    protected void doHead(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException {
        handleRequest(req, resp);
    }
    
    
    private void handleRequest(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        logger.info(req.getMethod() + " called on " + req.getRequestURI());
        
        if ("true".equals(req.getParameter("exception"))) { //trigger exception
            writeError(req, resp);
        } else if ("true".equals(req.getParameter("stream"))) {
            int streamTime = 10000;
            int streamChunkCount = 1000;
            resp.setContentType("text/plain");
            for (int i = 0; i < streamChunkCount; i ++) {
                resp.getOutputStream().print("lolollllllllllllollllllllllllolololololooloolool");
                try {
                    Thread.sleep(streamTime / streamChunkCount);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            
        } else {
            
            long waitDuration  = 200;
            
            BufferedReader reader = req.getReader();
            int lineCount = 0;
            while (reader.readLine() != null) { //read the input
                lineCount ++;
            }
            
            logger.info(lineCount);
            
            if (req.getParameter("duration") != null) {
                waitDuration = Long.parseLong(req.getParameter("duration"));
            }
            
            //artificial wait...better trace extends this way
            try {
                Thread.sleep(waitDuration);
            } catch (InterruptedException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
            
            if ("true".equals(req.getParameter("redirect")) && !req.getRequestURI().endsWith("/redirected")) { //trigger redirect
                logger.info("Redirecting...");
                resp.sendRedirect("redirected");
            } else {
                writeResponse(req, resp, "true".equals(req.getParameter("chunked")));
            }
        }
    }

    private void writeResponse(HttpServletRequest req, HttpServletResponse resp, boolean chunked) throws IOException {
//      Map<String, String> map = new HashMap<String, String>();
//      
//      map.put("method", req.getMethod());
//      map.put("path", req.getRequestURI());
//      
//      
//      JSONObject jsonObject = new JSONObject();
//      try {
//          jsonObject.put("result", new JSONObject(map));
//      } catch (JSONException e) {
//          // TODO Auto-generated catch block
//          e.printStackTrace();
//      }
      
        String jsonString = "{\"result\":{ \"method\":\"" + req.getMethod() + "\", \"path\":\"" + req.getRequestURI() + "\" } }";
        
        resp.setCharacterEncoding("utf-8");
        resp.setContentType("application/json");
        
        //resp.getOutputStream().write(jsonObject.toString().getBytes());
        resp.getOutputStream().write(jsonString.getBytes());
        
        if (chunked) {
            resp.flushBuffer();
            //send in more stuff
            Random random = new Random();
            for (int i = 0 ; i < 1000; i++) {
                byte[] nextBytes = new byte[1024];
                random.nextBytes(nextBytes);
                resp.getOutputStream().write(nextBytes);
            }
        }
                
        resp.getOutputStream().close();
    }
    

    private void writeError(HttpServletRequest req, HttpServletResponse resp) throws IOException {
//        JSONObject jsonObject = new JSONObject();
//        try {
//            jsonObject.put("response", "error");
//        } catch (JSONException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
        
        String jsonString = "{\"response\":\"error\"}";
        
        resp.setCharacterEncoding("utf-8");
        resp.setContentType("application/json");
        resp.setStatus(500);
        
        //resp.getOutputStream().write(jsonObject.toString().getBytes());
        resp.getOutputStream().write(jsonString.getBytes());
        
        resp.getOutputStream().close();
    }
    
}
