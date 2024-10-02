package com.tracelytics.test.action;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.redisson.Config;
import org.redisson.Redisson;
import org.redisson.SingleServerConfig;
import org.redisson.connection.ConnectionManager;
import org.redisson.connection.SingleConnectionManager;
import org.redisson.core.RBucket;
import org.redisson.core.RList;
import org.redisson.core.RMap;
import org.redisson.core.RSet;
import org.redisson.core.RSortedSet;

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
    
    protected static Redisson redisson;
    protected static ConnectionManager connectionManager;
    
    private static final int LIST_SIZE = 100;
    protected static final String[] LIST = new String[LIST_SIZE];
    
    static {
        initializeRedis();
        
        for (int i = 0; i < LIST_SIZE; i++) {
            LIST[i] = String.valueOf(Math.random());
        }
    }

    private List<String> extendedOutput;

    protected AbstractRedissonAction() {}
    

    protected static boolean initializeRedis() {
        Config config = new Config();
        SingleServerConfig singleServerConfig = config.useSingleServer().setAddress(REDIS_HOST + ":" + REDIS_PORT);
        
        redisson = Redisson.create(config);
        
        redisson.flushdb();
        
        populate(redisson);
        
        connectionManager = new SingleConnectionManager(singleServerConfig, config);
        
        return true;
    }
    
    private static void populate(Redisson redisson) {
        RBucket<String> bucket = redisson.getBucket(KEY);
        bucket.set(VALUE);
        
        RList<String> list = redisson.getList(LIST_KEY);
        list.add(VALUE);
        
        RSet<String> set = redisson.getSet(SET_KEY);
        set.add(VALUE);
        
//        RSortedSet<String> sortedSet = redisson.getSortedSet(ZSET_KEY); //not going to sorted set (Z) as expected
        
//        sortedSet.add(VALUE);
        
        RMap<String, String> map = redisson.getMap(HASH_KEY);
        map.put(HASH_FIELD_KEY, VALUE);
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
