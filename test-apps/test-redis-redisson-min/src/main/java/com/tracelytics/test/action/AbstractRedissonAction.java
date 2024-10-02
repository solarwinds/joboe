package com.tracelytics.test.action;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.redisson.Config;
import org.redisson.Redisson;
import org.redisson.connection.ConnectionManager;

import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisConnection;
import com.opensymphony.xwork2.ActionSupport;
import com.opensymphony.xwork2.Preparable;

@SuppressWarnings("serial")
public abstract class AbstractRedissonAction extends ActionSupport implements Preparable {
    protected static final Object NON_SERIALIZABLE_KEY = new Object();
    protected static final String KEY = "test-string-key";
    protected static final String KEY_2 = "test-string-key-2";
    protected static final String VALUE = "1";
    
    protected static final String LIST_KEY = "test-list-string-key";
    
    protected static final String SET_KEY = "test-set-string-key";
    
    protected static final String ZSET_KEY = "test-zset-string-key";
    
    protected static final String LOG_KEY = "test-log-string-key";
    
    protected static final String HASH_KEY = "test-hash-string-key";
    
    protected static final String HASH_FIELD_KEY = "test-hash-field-string-key";
    
    protected static final int TIMEOUT = 100;
    protected static final int EXPIRY = 100;
    
    protected static final String REDIS_HOST = "localhost";
    protected static final String REDIS_PORT = "6379";
    
    private static final int LIST_SIZE = 100;
    protected static final String[] LIST = new String[LIST_SIZE];
    
    static {
        initializeRedis();
        
        for (int i = 0; i < LIST_SIZE; i++) {
            LIST[i] = String.valueOf(Math.random());
        }
    }
    
    protected static Config config;

    private List<String> extendedOutput;

    protected AbstractRedissonAction() {}
    

    protected static boolean initializeRedis() {
        config = new Config();
        config.addAddress(REDIS_HOST + ":" + REDIS_PORT);
        
        ConnectionManager connectionManager = new ConnectionManager(config);
        try {
            connectionManager.connection().flushall();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        populate(connectionManager.<String, String>connection());
        
        connectionManager.shutdown();
        
        return true;
    }
    
    private static void printList(List<?> keys) {
        System.out.println(keys);
        for (Object key : keys) {
            System.out.println(key.getClass().getName());
        }
        
    }


    private static void populate(RedisConnection<String, String> connection) {
        connection.set(KEY, VALUE);
        connection.lpush(LIST_KEY, VALUE, VALUE);
        connection.hset(HASH_KEY, HASH_FIELD_KEY, VALUE);
        connection.zadd(ZSET_KEY, 1.0, VALUE, 2.0, VALUE);
    }

    @Override
    public String execute() throws Exception {
        try {
             return test();
        } catch (Exception e) {
            printToOutput(e.getMessage(), e.getStackTrace());
            e.printStackTrace();
            return SUCCESS;
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

    protected abstract String test() throws Exception;
    
    
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
