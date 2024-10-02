package com.tracelytics.test.action;

import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.convention.annotation.Results;

import com.opensymphony.xwork2.ActionSupport;

import net.rubyeye.xmemcached.MemcachedClient;
import net.rubyeye.xmemcached.XMemcachedClientBuilder;
import net.rubyeye.xmemcached.utils.AddrUtil;

@Results({
    @Result(name="success", location="index.jsp"),
    @Result(name="error", location="index.jsp"),
    @Result(name="input", location="index.jsp")
})
public class TestManyGet extends ActionSupport {
    private static final String DEFAULT_SERVER = "localhost:11211";
    private static final String TEST_KEY = "test-key";
    
    private String server;
    
    public String getServer() {
        return server;
    }
    
    public void setServer(String server) {
        this.server = server;
    }
    
    public String execute() throws Exception {
        String server = this.server != null ? this.server : DEFAULT_SERVER;
        XMemcachedClientBuilder builder = new XMemcachedClientBuilder(AddrUtil.getAddresses(server));
        
        MemcachedClient client = builder.build();

        client.set(TEST_KEY, 10, Math.random()); 
        
        for (int i = 0 ; i < 100; i ++) {
            client.get(TEST_KEY);
        }
                    
        client.shutdown();
        
        return SUCCESS;
    }
}
