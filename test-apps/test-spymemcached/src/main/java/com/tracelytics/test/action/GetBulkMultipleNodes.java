package com.tracelytics.test.action;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import net.spy.memcached.BinaryConnectionFactory;
import net.spy.memcached.DefaultConnectionFactory;
import net.spy.memcached.MemcachedClient;

import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.convention.annotation.Results;

import com.opensymphony.xwork2.ActionSupport;


@Results({
    @Result(name="success", location="index.jsp"),
    @Result(name="error", location="index.jsp"),
    @Result(name="input", location="index.jsp")
})
public class GetBulkMultipleNodes extends ActionSupport {
    private static int COUNT = 10;
    private String connectionType;
    
    public String getConnectionType() {
        return connectionType;
    }

    public void setConnectionType(String connectionType) {
        this.connectionType = connectionType;
    }

    public String execute() throws Exception {
        List<InetSocketAddress> addresses = new ArrayList<InetSocketAddress>();
        
        addresses.add(new InetSocketAddress("172.16.124.228", 11211)); //TODO change this to the 2nd server location and uncomment for multiple node testing
        addresses.add(new InetSocketAddress("localhost", 11211));
        //172.16.124.228 is currently the vm3 in the Vancouver office. Take note that usually class net.spy.memcached.ArrayModNodeLocator's getServerForKey(String key)
        //is used to determine which server it should look up
        boolean isBinary = "1".equals(connectionType);
        
        
        MemcachedClient client = new MemcachedClient(isBinary? new BinaryConnectionFactory() : new DefaultConnectionFactory(), addresses);
        
        List<String> keys = new ArrayList<String>();
        
        for (int i = 0 ; i < COUNT; i++) {
            keys.add("test-" + Math.random());
        }
        
        Random random = new Random();
        for (String key : keys) {
            client.set(key, COUNT, "testing").get();
        }
        
        
        Map<String, Object> result = client.getBulk(keys);
        
        System.out.println(result);
               
        addActionMessage("Successful!");
        
        return SUCCESS;
    }
}
