package com.tracelytics.test.action;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Protocol;

import com.opensymphony.xwork2.ActionSupport;
import com.opensymphony.xwork2.Preparable;

@SuppressWarnings("serial")
public abstract class AbstractJedisAction extends ActionSupport implements Preparable {
    protected static final Object NON_SERIALIZABLE_KEY = new Object();
    protected static final String STRING_KEY = "test-string-key";
    protected static final String STRING_KEY_2 = "test-string-key-2";
    protected static final String STRING_VALUE = "1";
    
    protected static final String LIST_STRING_KEY = "test-list-string-key";
    
    protected static final String SET_STRING_KEY = "test-set-string-key";
    
    protected static final String ZSET_STRING_KEY = "test-zset-string-key";
    
    protected static final String LOG_STRING_KEY = "test-log-string-key";
    
    protected static final String HASH_STRING_KEY = "test-hash-string-key";
    
    protected static final String HASH_FIELD_STRING_KEY = "test-hash-field-string-key";
    
    protected static final int TIMEOUT = 100;
    protected static final int EXPIRY = 100;
    
    protected static final String REDIS_HOST = "localhost";
    protected static final int REDIS_PORT = Protocol.DEFAULT_PORT;
    
    protected static JedisPool jedisPool;

    static {
        initialize();
    }

    private List<String> extendedOutput;

    protected AbstractJedisAction() {}

    protected static boolean initialize() {
        jedisPool = new JedisPool(REDIS_HOST);
        jedisPool.init();
        Jedis jedis = null;
        
        try {
            jedis = jedisPool.getResource(); 
            jedis.flushAll();
            populate(jedis);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            if (jedis != null) {
                try {
                    jedis.disconnect();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        
        return true;
    }
    
        
    private static void populate(Jedis jedis) {
        jedis.set(STRING_KEY, STRING_VALUE);
        
        jedis.rpush(LIST_STRING_KEY, STRING_VALUE);
        jedis.rpush(LIST_STRING_KEY, STRING_VALUE);
        
        jedis.sadd(SET_STRING_KEY, STRING_VALUE);
        
        jedis.zadd(ZSET_STRING_KEY, 1, STRING_VALUE);
        
        jedis.hset(HASH_STRING_KEY, HASH_FIELD_STRING_KEY, STRING_VALUE);
    }

    @Override
    public String execute() throws Exception {
        Jedis jedis = null;
            
        try {
            jedis = jedisPool.getResource(); 
            return test(jedis);
        } catch (Exception e) {
            printToOutput(e.getMessage(), e.getStackTrace());
            e.printStackTrace();
            return SUCCESS;
        } finally {
            if (jedis != null) {
                try {
                    jedis.disconnect();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        
    }

    public List<String> getExtendedOutput() {
        return extendedOutput;
    }

    public void setExtendedOutput(List<String> extendedOutput) {
        this.extendedOutput = extendedOutput;
    }

    public void appendExtendedOutput(String text) {
        if (extendedOutput == null) {
            extendedOutput = new LinkedList<String>();
        }
        extendedOutput.add(text);
    }

    @Override
    public void prepare() throws Exception {
        extendedOutput = null; //clear the output       
    }

    protected abstract String test(Jedis jedis) throws Exception;
    
    
    protected void printToOutput(String title, Map<?, ?> map) {
        if (title != null) {
            appendExtendedOutput(title);
        }
        
        for (Object element : map.entrySet()) {
            appendExtendedOutput(element != null ? element.toString() : "null");
        }
    }
    
    protected void printToOutput(String title, List<?> keys) {
        if (title != null) {
            appendExtendedOutput(title);
        }
        
        for (Object element : keys) {
            appendExtendedOutput(element != null ? element.toString() : "null");
        }
    }
    
    protected void printToOutput(String title, Object...items) {
        printToOutput(title, Arrays.asList(items));
    }
    

}
