package com.tracelytics.test.action;

import java.util.Arrays;

import net.rubyeye.xmemcached.CASOperation;
import net.rubyeye.xmemcached.GetsResponse;
import net.rubyeye.xmemcached.MemcachedClient;
import net.rubyeye.xmemcached.XMemcachedClientBuilder;
import net.rubyeye.xmemcached.command.BinaryCommandFactory;
import net.rubyeye.xmemcached.transcoders.SerializingTranscoder;
import net.rubyeye.xmemcached.transcoders.Transcoder;
import net.rubyeye.xmemcached.utils.AddrUtil;

import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.convention.annotation.Results;

import com.opensymphony.xwork2.ActionSupport;


@Results({
    @Result(name="success", location="index.jsp"),
    @Result(name="error", location="index.jsp"),
    @Result(name="input", location="index.jsp")
})
public class TestAll extends ActionSupport {
    private static final int MAX_DURATION = 30 * 24 * 60 * 60;
    private static final int TIME_OUT = 1000;
    
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
        
        XMemcachedClientBuilder builder = new XMemcachedClientBuilder(AddrUtil.getAddresses("localhost:11211"));
        if (isBinary) {
            builder.setCommandFactory(new BinaryCommandFactory());
        }
        MemcachedClient client = builder.build();

        client.set(key, duration, value); 
        client.set("numTest", 30, "0");
        
        Transcoder<Object> sampleTranscoder = new SerializingTranscoder();
//        Collection<Transcoder<Object>> transcoders = new HashSet<Transcoder<Object>>();
//        transcoders.add(sampleTranscoder);
        
        client.add(key, duration, value);
        client.add(key, duration, value, TIME_OUT);
        client.add(key, duration, value, sampleTranscoder);
        client.add(key, duration, value, sampleTranscoder, TIME_OUT);
        
        client.append(key, "*");
        client.append(key, "*", TIME_OUT);
        client.appendWithNoReply(key, "*");
        
        GetsResponse<Object> getsResponse = client.gets("numTest");
        CASOperation<Object> casOperation = new CASOperation<Object>() {
            public int getMaxTries() {
                return 1;
            }
            public Object getNewValue(long currentCAS, Object currentValue) {
                return "0";
            }
            
        };
        
        
        client.cas(key, casOperation);
        client.cas(key, getsResponse, casOperation);
        client.cas(key, duration, casOperation);
        client.cas(key, duration, casOperation, sampleTranscoder);
        client.cas(key, duration, getsResponse, casOperation);
        client.cas(key, duration, "0", getsResponse.getCas());
        client.cas(key, duration, getsResponse, casOperation, sampleTranscoder);
        client.cas(key, duration, "0", TIME_OUT, getsResponse.getCas());
        client.cas(key, duration, "0", sampleTranscoder, getsResponse.getCas());
        client.cas(key, duration, "0", sampleTranscoder, TIME_OUT, getsResponse.getCas());
        
        client.casWithNoReply(key, casOperation);
        client.casWithNoReply(key, getsResponse, casOperation);
        client.casWithNoReply(key, duration, casOperation);
        client.casWithNoReply(key, duration, getsResponse, casOperation);
        
        client.decr("numTest", 1);
        client.decr("numTest", 2, 0);
        client.decr("numTest", 2, 0, TIME_OUT);
        client.decrWithNoReply("numTest", 1);
        
        client.delete(key);
        client.deleteWithNoReply(key);
        
        client.set(key, duration, value); 
        client.get(Arrays.asList(key, "notexist", "notexist2", "notexist3", "notexist4", "notexist5"));
        client.get(Arrays.asList(key, "notexist", "notexist2", "notexist3", "notexist4", "notexist5"), TIME_OUT);
        client.get(Arrays.asList(key, "notexist", "notexist2", "notexist3", "notexist4", "notexist5"), TIME_OUT, sampleTranscoder);
        client.get(Arrays.asList(key, "notexist", "notexist2", "notexist3", "notexist4", "notexist5"), sampleTranscoder);
        client.get(key);
        client.get(key, TIME_OUT);
        client.get(key, sampleTranscoder);
        client.get(key, TIME_OUT, sampleTranscoder);
        
        client.getCounter("numTest");
        client.getCounter("numTest", 0);
        
        client.gets(Arrays.asList(key, "notexist", "notexist2", "notexist3", "notexist4", "notexist5"));
        client.gets(Arrays.asList(key, "notexist", "notexist2", "notexist3", "notexist4", "notexist5"), TIME_OUT);
        client.gets(Arrays.asList(key, "notexist", "notexist2", "notexist3", "notexist4", "notexist5"), TIME_OUT, sampleTranscoder);
        client.gets(Arrays.asList(key, "notexist", "notexist2", "notexist3", "notexist4", "notexist5"), sampleTranscoder);
        client.gets(key);
        client.gets(key, TIME_OUT);
        client.gets(key, sampleTranscoder);
        client.gets(key, TIME_OUT, sampleTranscoder);
        
        client.incr("numTest", 1);
        client.incr("numTest", 2, 0);
        client.incr("numTest", 2, 0, TIME_OUT);
        client.incrWithNoReply("numTest", 1);
        
        client.prepend(key, "-");
        client.prepend(key, "-", TIME_OUT);
        client.prependWithNoReply(key, "-");
                
        client.replace(key, duration, value);
        client.replace(key, duration, value, TIME_OUT);
        client.replace(key, duration, value, sampleTranscoder);
        client.replace(key, duration, value, sampleTranscoder, TIME_OUT);
        client.replaceWithNoReply(key, duration, value);
        client.replaceWithNoReply(key, duration, value, sampleTranscoder);
        
        client.set(key, duration, value);
        client.set(key, duration, value, TIME_OUT);
        client.set(key, duration, value, sampleTranscoder);
        client.set(key, duration, value, sampleTranscoder, TIME_OUT);
        
        client.setWithNoReply(key, duration, value);
        client.setWithNoReply(key, duration, value, sampleTranscoder);
        
        client.flushAll();
        client.flushAll(TIME_OUT);
        
        addActionMessage("Successful!");
        
        return SUCCESS;
    }
}
