package com.tracelytics.test;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.ws.AsyncHandler;
import javax.xml.ws.Response;
import javax.xml.ws.WebServiceException;

import sample.com.GetIntResponse;
import sample.com.OperatorService;

import com.appoptics.api.ext.LogMethod;


/**
 * Tests sample rest server deployed locally, please see README.txt
 * @author Patson Luk
 *
 */
public class SampleSoapAsyncServlet extends HttpServlet {


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException {
        
              
        //test sample SOAP (own server with x-trace id in response header). Require starting the local SOAP server, please see README.TXT
        try {
            testSampleSoap(); 
           
            req.setAttribute("clientAction", "Sample API (SOAP Async)");
            
        } catch (WebServiceException e) {
            req.setAttribute("failureMessage", e.getMessage());
        } catch (InterruptedException e) {
            req.setAttribute("failureMessage", e.getMessage());
        } catch (ExecutionException e) {
            req.setAttribute("failureMessage", e.getMessage());
        }
        
        req.getRequestDispatcher("sample.jsp").forward(req, resp);
    }

    private void testSampleSoap() throws WebServiceException, InterruptedException, ExecutionException {
        Response<GetIntResponse> response1 = new OperatorService().getSampleSoapPort().getIntAsync(5);
        new OperatorService().getSampleSoapPort().getIntAsync(10, new AsyncHandler<GetIntResponse>() {
            public void handleResponse(Response<GetIntResponse> res) {
                try {
                    System.out.println(res.get().getReturn());
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                
            }
        });
        
        System.out.println(response1.get().getReturn());
    }
    
}
