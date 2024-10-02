package com.tracelytics.test;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.solr.client.solrj.SolrServerException;

public class TestServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException {
        String id = req.getParameter("id");
        String name = req.getParameter("name");
        String path = req.getParameter("path");
        String solrServerUrl = req.getParameter("solrServerUrl");
        if (path != null) {
            solrServerUrl += path;
        }
        
        if (id != null && !"".equals(id) && name != null && !"".equals(name) && solrServerUrl != null && !"".equals(solrServerUrl)) {
            try {
                SolrDriver.performInsert(solrServerUrl, id, name);
                req.setAttribute("success", Boolean.TRUE);
            } catch (SolrServerException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                req.setAttribute("success", Boolean.FALSE);                
            }
        } else {
            req.setAttribute("success", Boolean.FALSE);
        }
        
        req.getRequestDispatcher("index.jsp").forward(req, resp);
    }
    
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException {
        doGet(req, resp);
    }
}
