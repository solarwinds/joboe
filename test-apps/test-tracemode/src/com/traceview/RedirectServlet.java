package com.traceview;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet implementation class RedirectServlet
 */
public class RedirectServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public RedirectServlet() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String mode = request.getParameter("mode");
		HttpClient httpclient = HttpClients.createDefault();
		HttpGet httpGet;
		if (mode == null) {
			httpGet = new HttpGet("http://localhost:8080/");
		} else {
			httpGet = new HttpGet("http://localhost:8080/" + mode + ".html");
		}
		HttpResponse response1 = httpclient.execute(httpGet);
		
		BufferedReader rd = new BufferedReader
				  (new InputStreamReader(response1.getEntity().getContent()));
				    
		String line = "";
		while ((line = rd.readLine()) != null) {
			response.getWriter().println(line);
		}
	}
}
