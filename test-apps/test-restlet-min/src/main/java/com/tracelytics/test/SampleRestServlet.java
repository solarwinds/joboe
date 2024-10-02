package com.tracelytics.test;

import java.io.IOException;
import java.net.URI;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.resource.ClientResource;
import org.restlet.resource.ResourceException;


/**
 * Tests sample rest server deployed locally, please see README.txt
 * @author Patson Luk
 *
 */
public class SampleRestServlet extends HttpServlet {


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException {
        
        //REST CXF Proxy on our own sample server
        try {
            ClientResource client = new ClientResource(new URI("http://localhost:8080/test-rest-server/Default?duration=100"));
            
            client.get();
            client.release();
            client.get(String.class);
            client.release();
            client.get(MediaType.APPLICATION_ALL);
            client.release();
            
            Form form = new Form();
            form.add("postParam", "test");
            client.post(form);
            client.release();
            
            org.restlet.service.ConverterService cs = client.getConverterService();
            client.post(cs.toRepresentation(form, null, client));
            client.release();
            client.post(form, String.class);
            client.release();
            client.post(form, MediaType.APPLICATION_ALL);
            client.release();
            
            client.delete();
            client.release();
            client.delete(String.class);
            client.release();
            client.delete(MediaType.APPLICATION_ALL);
            client.release();
            
            client.head();
            client.release();
            client.head(MediaType.APPLICATION_ALL);
            client.release();
            
            client.options();
            client.release();
            client.options(String.class);
            client.release();
            client.options(MediaType.APPLICATION_ALL);
            client.release();
            
            
            
            client.put(form);
            client.release();
            client.put(cs.toRepresentation(form, null, client));
            client.release();
            client.put(form, String.class);
            client.release();
            client.put(form, MediaType.APPLICATION_ALL);
            client.release();
            
            req.setAttribute("clientAction", "SAMPLE API (REST)");
            
        } catch (Exception e) {
            e.printStackTrace();
            req.setAttribute("failureMessage", e.getMessage() != null ? e.getMessage() : e.toString());
        }
        
        req.getRequestDispatcher("simple.jsp").forward(req, resp);
    }
}
