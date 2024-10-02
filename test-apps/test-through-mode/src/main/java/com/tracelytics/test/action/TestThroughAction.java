package com.tracelytics.test.action;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.convention.annotation.Results;

import com.opensymphony.xwork2.ActionSupport;


@SuppressWarnings("serial")
@Results({
    @Result(name="success", location="index.jsp"),
    @Result(name="error", location="index.jsp"),
})
public class TestThroughAction extends ActionSupport {
    private String targetUri;
        
    @Override
    public String execute() throws Exception {
        HttpClient client = new DefaultHttpClient();
        
        HttpGet get = new HttpGet(targetUri);
        HttpResponse response = client.execute(get);
        
        addActionMessage("Response status [" + response.getStatusLine() + "]");
        
        return SUCCESS;
    }   
    
    
    public String getTargetUri() {
        return targetUri;
    }

    public void setTargetUri(String targetUri) {
        this.targetUri = targetUri;
    }
}
