package com.tracelytics.test.action;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import net.spy.memcached.BinaryConnectionFactory;
import net.spy.memcached.DefaultConnectionFactory;
import net.spy.memcached.MemcachedClient;
import net.spy.memcached.internal.GetFuture;

import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.convention.annotation.Results;

import com.opensymphony.xwork2.ActionSupport;


@Results({
    @Result(name="success", location="index.jsp"),
    @Result(name="error", location="index.jsp"),
    @Result(name="input", location="index.jsp")
})
public class AsyncSpecial extends ActionSupport {
    private String connectionType;
    
    public String getConnectionType() {
        return connectionType;
    }

    public void setConnectionType(String connectionType) {
        this.connectionType = connectionType;
    }

    public String execute() throws Exception {
        List<InetSocketAddress> addresses = new ArrayList<InetSocketAddress>();
        
//        addresses.add(new InetSocketAddress("172.16.124.228", 11211)); //TODO change this to the 2nd server location and uncomment for multiple node testing
        addresses.add(new InetSocketAddress("localhost", 11211));
        //172.16.124.228 is currently the vm3 in the Vancouver office. Take note that usually class net.spy.memcached.ArrayModNodeLocator's getServerForKey(String key)
        //is used to determine which server it should look up
        boolean isBinary = "1".equals(connectionType);
        
        
        MemcachedClient client = new MemcachedClient(isBinary? new BinaryConnectionFactory() : new DefaultConnectionFactory(), addresses);
        
        GetFuture<Object> resultFuture = client.asyncGet("numtest");
        try {
            System.out.println(resultFuture.get(1, TimeUnit.NANOSECONDS));
        } catch (TimeoutException e) {
            //expected
            System.out.println("Found expected timeout");
        }
        
        resultFuture = client.asyncGet("numtest");
        resultFuture.cancel(true);
        
        return SUCCESS;
    }
}
