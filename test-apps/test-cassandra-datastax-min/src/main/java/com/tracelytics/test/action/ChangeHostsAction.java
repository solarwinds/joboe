package com.tracelytics.test.action;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.convention.annotation.Results;

import com.datastax.driver.core.Session;
   
@Results({
    @Result(name="success", location="index.jsp"),
    @Result(name="error", location="index.jsp"),
    @Result(name="input", location="index.jsp"),
})
public class ChangeHostsAction extends AbstractCqlAction {
    private static final String DEFAULT_HOST_STRING = "localhost";
    private String hostString = DEFAULT_HOST_STRING;
    private String[] hosts;
    
    @Override
    public String execute() throws Exception {
        if (AbstractCqlAction.initialize(hosts)) {
            addActionMessage("Switched to " + hostString);
        } else {
            addActionError("Failed to switch to " + hostString);
        }
        
        return SUCCESS;
    }
    
    @Override
    protected String test(Session session) {
        return SUCCESS;
    }
    
    
    public String getHostString() {
        return hostString;
    }
    
    public void setHostString(String hostString) {
        this.hostString = hostString;
    }
    
    @Override
    public void validate() {
        super.validate();
        hosts = hostString.split(";");
        if (hosts.length == 0) {
            addFieldError("hostString", "Must contain at least 1 host");
            hostString = DEFAULT_HOST_STRING;
        }
        
        for (int i = 0 ; i < hosts.length; i++) { //test whether hosts are valid
            hosts[i] = hosts[i].trim();
            String host = hosts[i];
            try {
                InetAddress.getByName(host.trim());
            } catch (UnknownHostException e) {
                addFieldError("hostString", "[" + host + "] is not a valid host!");
            }
        }
    }
}
