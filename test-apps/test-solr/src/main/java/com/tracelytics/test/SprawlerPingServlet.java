package com.tracelytics.test;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import com.tracelytics.test.sprawler.Sprawler;
import com.tracelytics.test.sprawler.Sprawler.Status;
import com.tracelytics.test.sprawler.SprawlerManager;

public class SprawlerPingServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        Map<String, Object> values = new HashMap<String, Object>();
        
        if (req.getParameter("id") != null) {
            UUID uuid = UUID.fromString(req.getParameter("id"));
            
            Sprawler sprawler = SprawlerManager.getSprawler(uuid);
            
            
            if (sprawler != null) {
//                req.setAttribute("collected", sprawler.getCollectedLinks());
//                req.setAttribute("isCompleted", sprawler.getStatus() == Status.COMPLETED);
//                values.put("collected", sprawler.getCollectedLinks().keySet());
                values.put("collected", sprawler.getCollectedLinks());
//                Map<String, String> map = new HashMap<String, String>();
//                map.put("1", "one");
//                map.put("2", "two");
//                values.put("collected", map);
                
                values.put("isCompleted", sprawler.getStatus() == Status.COMPLETED);
            } else {
//                req.setAttribute("isCompleted", true);
                values.put("isCompleted", true);
            }
        } else {
            values.put("isCompleted", true);   
        }
        
        JSONObject jsonObject = new JSONObject(values);
        
        resp.setContentType("application/json");
        
        PrintWriter out = resp.getWriter();
        out.print(jsonObject);
        out.flush();
                
//        req.getRequestDispatcher("sprawlerPing.jsp").forward(req, resp);
    }
    
}
