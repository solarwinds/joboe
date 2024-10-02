package com.appoptics.test.controller;

import org.redisson.Redisson;
import org.redisson.api.*;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.redisson.connection.ConnectionManager;
import org.redisson.connection.SingleConnectionManager;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

@SuppressWarnings("serial")
public abstract class AbstractRedissonController {
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

    protected static final float TIMEOUT = 100f;
    protected static final int EXPIRY = 100;

//    protected static final String host = "ec2-52-7-124-5.compute-1.amazonaws.com";
//    protected static final int port = 1240;
//    protected static final String host = "localhost";
//    protected static final int port = 6379;
    protected static final String DEFAULT_HOST = "localhost";
    protected static final int DEFAULT_PORT = 6379;

    protected static String host = DEFAULT_HOST;
    protected static int port = DEFAULT_PORT;

//    protected static RedissonClient client;
//    protected static RedissonReactiveClient reactiveClient;
//    protected static ConnectionManager connectionManager;

    private static final int LIST_SIZE = 100;
    protected static final String[] LIST = new String[LIST_SIZE];

    static {
        for (int i = 0; i < LIST_SIZE; i++) {
            LIST[i] = String.valueOf(Math.random());
        }
    }

    private List<String> extendedOutput;

    protected AbstractRedissonController() {}

    protected RedissonClient getClient() {
        Config config = new Config();
        try {
            config.useSingleServer().setAddress("redis://" + host + ":" + port);

            RedissonClient client = Redisson.create(config);
            client.getExecutorService(RExecutorService.MAPREDUCE_NAME).registerWorkers(3);

            return client;
        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        }
    }

    protected RedissonReactiveClient getReactiveClient() {
        Config config = new Config();
        try {
            config.useSingleServer().setAddress("redis://" + host + ":" + port);

            RedissonReactiveClient reactiveClient = Redisson.createReactive(config);
//            connectionManager = new SingleConnectionManager(singleServerConfig, config, UUID.randomUUID());
            return reactiveClient;
        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        }
    }
    

//    protected static boolean initializeRedis() {
//        Config config = new Config();
//        try {
//            SingleServerConfig singleServerConfig = config.useSingleServer().setAddress("redis://" + host + ":" + port);
//
//            client = Redisson.create(config);
//            reactiveClient = Redisson.createReactive(config);
//
//            populate(client);
//
//            connectionManager = new SingleConnectionManager(singleServerConfig, config, UUID.randomUUID());
//
//            client.getExecutorService(RExecutorService.MAPREDUCE_NAME).registerWorkers(3);
//
//            return true;
//        } catch (Throwable e) {
//            e.printStackTrace();
//            return false;
//        }
//    }
//
//    private static void populate(RedissonClient client) {
//        RBucket<String> bucket = client.getBucket(KEY);
//        bucket.set(VALUE);
//
//        RList<String> list = client.getList(LIST_KEY);
//        list.add(VALUE);
//
//        RSet<String> set = client.getSet(SET_KEY);
//        set.add(VALUE);
//
////        RSortedSet<String> sortedSet = redisson.getSortedSet(ZSET_KEY); //not going to sorted set (Z) as expected
//
////        sortedSet.add(VALUE);
//
//        RMap<String, String> map = client.getMap(HASH_KEY);
//        map.put(HASH_FIELD_KEY, VALUE);
//    }

    // Total control - setup a model and return the view name yourself. Or
    // consider subclassing ExceptionHandlerExceptionResolver (see below).
    @ExceptionHandler(Exception.class)
    public ModelAndView handleError(HttpServletRequest req, Exception ex) {
        printToOutput(ex.getMessage(), ex.getStackTrace());
        ex.printStackTrace();

        ModelAndView result = getModelAndView("index");
        result.addObject("exception", ex);
        return result;
    }

    public List<String> getExtendedOutput() {
        return extendedOutput;
    }

    public void clearExtendedOutput() {
        if (extendedOutput != null) {
            extendedOutput.clear();
        }
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

    protected ModelAndView getModelAndView(String viewName) {
        ModelAndView result = new ModelAndView();
        result.addObject("extendedOutput", extendedOutput);
        result.setViewName(viewName);
        return result;
    }



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

    protected void iterateCollection(Collection<?> collection) {
        iterateCollection(collection.iterator());
    }

    protected void iterateCollection(Iterator<?> iter) {
        while (iter.hasNext()) {
            Object value = iter.next();
            System.out.println(value + " : " + value.getClass().getName());
        }
    }
}
