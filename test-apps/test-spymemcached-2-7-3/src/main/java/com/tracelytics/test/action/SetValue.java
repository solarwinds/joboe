package com.tracelytics.test.action;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;

import net.spy.memcached.BinaryConnectionFactory;
import net.spy.memcached.CASValue;
import net.spy.memcached.DefaultConnectionFactory;
import net.spy.memcached.MemcachedClient;
import net.spy.memcached.transcoders.SerializingTranscoder;
import net.spy.memcached.transcoders.Transcoder;

import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.convention.annotation.Results;

import com.opensymphony.xwork2.ActionSupport;


@Results({
    @Result(name="success", location="index.jsp"),
    @Result(name="error", location="index.jsp"),
    @Result(name="input", location="index.jsp")
})
public class SetValue extends ActionSupport {
    private static final int MAX_DURATION = 30 * 24 * 60 * 60;
    
    private String key;
    private String durationString;
    private String value;
    private String connectionType;
    private int duration;
    
    
    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getDurationString() {
        return durationString;
    }

    public void setDurationString(String durationString) {
        this.durationString = durationString;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
    
    public String getConnectionType() {
        return connectionType;
    }

    public void setConnectionType(String connectionType) {
        this.connectionType = connectionType;
    }

    
    @Override
    public void validate() {
        if ("".equals(key.trim())) {
            addFieldError("key", "key cannot be empty");
        }
        
        if ("".equals(durationString.trim())) {
            addFieldError("durationString", "duration cannot be empty");
        } else { 
            try {
                duration = Integer.parseInt(durationString); 
                if (duration <= 0 || duration > MAX_DURATION) {
                    addFieldError("durationString", "invalid duration");
                }
            } catch (NumberFormatException e) {
                addFieldError("durationString", "invalid duration");
            }
        }
    }
    

    
    
    
    public String execute() throws Exception {
        boolean isBinary = "1".equals(connectionType);
        
        List<InetSocketAddress> addresses = new ArrayList<InetSocketAddress>();
        addresses.add(new InetSocketAddress("localhost", 11211));
//        addresses.add(new InetSocketAddress("172.16.124.228", 11211)); //TODO change this to the 2nd server location and uncomment for multiple node testing
        
        MemcachedClient client = new MemcachedClient(isBinary ? new BinaryConnectionFactory() : new DefaultConnectionFactory(), addresses);
        
        client.set(key, duration, value).get(); //wait for this to finish otherwise later statements might fail
        client.set("numTest", 30, "0").get(); //wait for this to finish otherwise later statements might fail
        
        Transcoder<Object> sampleTranscoder = new SerializingTranscoder();
        
        client.add(key, duration, value);
        client.add(key, duration, value, sampleTranscoder);
        
        client.append(client.gets(key).getCas(), key, "*");
        client.append(client.gets(key).getCas(), key, "*", sampleTranscoder);
        
        CASValue<Object> casValue = client.gets("numTest");
        client.cas("numTest", casValue.getCas(), duration, casValue.getValue(), sampleTranscoder);
        client.cas("numTest", casValue.getCas(), casValue.getValue());
        client.cas("numTest", casValue.getCas(), casValue.getValue(), sampleTranscoder);
        
        client.asyncCAS("numTest", casValue.getCas(), duration, casValue.getValue(), sampleTranscoder);
        client.asyncCAS("numTest", casValue.getCas(), casValue.getValue());
        client.asyncCAS("numTest", casValue.getCas(), casValue.getValue(), sampleTranscoder);
        
        
        client.decr("numTest", 1);
        client.decr("numTest", 2, 0);
        client.decr("numTest", 2, 0, duration);
        
        client.asyncDecr("numTest", 1);
        
        client.delete("numTest");
        client.set("numTest", 30, "0");
        client.delete("numTest", 1);
        
        System.out.println(client.get(key));
        client.get(key, sampleTranscoder);
        
        client.get("notExist");
        
        
        client.asyncGet(key);
        client.asyncGet(key, sampleTranscoder);
        
        if (isBinary) { //methods within only supported by Binary connection
            client.getAndLock(key, duration);
            client.getAndLock(key, duration, sampleTranscoder);
            
            client.asyncGetAndLock(key, duration);
            client.asyncGetAndLock(key, duration, sampleTranscoder);
            
            client.getAndTouch(key, duration);
            client.getAndTouch(key, duration, sampleTranscoder);
            
            client.asyncGetAndTouch(key, duration);
            client.asyncGetAndTouch(key, duration, sampleTranscoder);
        }
                
        client.getBulk(Arrays.asList(key, "notexist", "notexist2", "notexist3", "notexist4", "notexist5"));
        client.getBulk(Collections.singleton(key));
        client.getBulk(Collections.singleton(key), sampleTranscoder);
        client.getBulk(key);
        client.getBulk(sampleTranscoder, key);
        
        client.asyncGetBulk(Arrays.asList(key, "notexist", "notexist2", "notexist3", "notexist4", "notexist5"));
        client.asyncGetBulk(Collections.singleton(key));
        client.asyncGetBulk(Collections.singleton(key), sampleTranscoder);
        client.asyncGetBulk(key);
        client.asyncGetBulk(sampleTranscoder, key);
        
        client.gets(key);
        client.gets(key, sampleTranscoder);
        
        client.asyncGets(key);
        client.asyncGets(key, sampleTranscoder);
        
        client.incr("numTest", 1);
        client.incr("numTest", 2, 0);
        client.incr("numTest", 2, 0, duration);
        
        client.asyncIncr("numTest", 1);
        
        client.prepend(client.gets(key).getCas(), key, "-");
        client.prepend(client.gets(key).getCas(), key, "-", sampleTranscoder);
        
        client.replace(key, duration, value);
        client.replace(key, duration, value, sampleTranscoder);
        
        Future<?> res = client.set(key, duration, value);
        client.set(key, duration, value, sampleTranscoder);
        
        if (isBinary) {
            client.touch(key, duration);
            client.touch(key, duration, sampleTranscoder);
        }
        
        client.flush();
        client.flush(1);
        
        addActionMessage("Successful!");
        
        return SUCCESS;
    }
}
