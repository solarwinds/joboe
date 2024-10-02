package com.tracelytics.test.action;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;


@SuppressWarnings("serial")
@org.apache.struts2.convention.annotation.Results({
    @org.apache.struts2.convention.annotation.Result(name="success", location="index.jsp"),
    @org.apache.struts2.convention.annotation.Result(name="error", location="index.jsp"),
})
public class TestUrlGetContent extends BaseTestAction {

    @Override
    public String execute() throws Exception {
        try {
            Object responseContent = new URL(getUrlString()).getContent();
            
            if (responseContent instanceof InputStream) {
                ((InputStream)responseContent).close();
            }
            
            printToOutput("object " + responseContent + " read");
            return SUCCESS;
        } catch (Exception e) {
            printToOutput(e.getMessage(), (Object[])e.getStackTrace());
            return SUCCESS;
        } 
    }

    @Override
    protected void preConnect(HttpURLConnection connection) throws IOException {
        // TODO Auto-generated method stub
        
    }
    
    

}
