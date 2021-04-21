package com.tracelytics.test.springboot;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;
import redis.clients.jedis.BinaryJedisPubSub;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

@SuppressWarnings("serial")
public abstract class AbstractJedisController {
    protected static final Object NON_SERIALIZABLE_KEY = new Object();
    protected static final String STRING_KEY = "test-string-key";
    protected static final byte[] BYTE_KEY = "test-byte-key".getBytes();
    protected static final String STRING_KEY_2 = "test-string-key-2";
    protected static final byte[] BYTE_KEY_2 = "test-byte-key-2".getBytes();
    protected static final String STRING_VALUE = "1";
    protected static final byte[] BYTE_VALUE = "2".getBytes();

    protected static final String LIST_STRING_KEY = "test-list-string-key";
    protected static final byte[] LIST_BYTE_KEY = "test-list-byte-key".getBytes();

    protected static final String SET_STRING_KEY = "test-set-string-key";
    protected static final byte[] SET_BYTE_KEY = "test-set-byte-key".getBytes();

    protected static final String ZSET_STRING_KEY = "test-zset-string-key";
    protected static final byte[] ZSET_BYTE_KEY = "test-zset-byte-key".getBytes();

    protected static final String LOG_STRING_KEY = "test-log-string-key";
    protected static final byte[] LOG_BYTE_KEY = "test-log-byte-key".getBytes();

    protected static final String HASH_STRING_KEY = "test-hash-string-key";
    protected static final byte[] HASH_BYTE_KEY = "test-hash-byte-key".getBytes();

    protected static final String HASH_FIELD_STRING_KEY = "test-hash-field-string-key";
    protected static final byte[] HASH_FIELD_BYTE_KEY = "test-hash-field-byte-key".getBytes();

    protected static final String STREAM_KEY = "test-stream-key";

    protected static final int TIMEOUT = 100;
    protected static final int EXPIRY = 100;

    protected static final String DEFAULT_HOST = "localhost";
    protected static final int DEFAULT_PORT = 6379;

    protected static String host = DEFAULT_HOST;
    protected static int port = DEFAULT_PORT;

    protected static JedisPool jedisPool;

    static {
        initialize();
    }

    private List<String> extendedOutput;

    protected static JedisPubSub subscription;
    protected static BinaryJedisPubSub binarySubscription;

    protected static boolean initialize() {
        jedisPool = new JedisPool(host, port);

        try (Jedis jedis = jedisPool.getResource()) {
            jedis.flushAll();
            populate(jedis);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        subscription = new DummyPubSub();
        binarySubscription = new DummyBinaryPubSub();

        return true;
    }

    private static void populate(Jedis jedis) {
        jedis.set(BYTE_KEY, BYTE_VALUE);
        jedis.set(STRING_KEY, STRING_VALUE);

        jedis.rpush(LIST_STRING_KEY, STRING_VALUE, STRING_VALUE);
        jedis.rpush(LIST_BYTE_KEY, BYTE_VALUE, BYTE_VALUE);

        jedis.sadd(SET_BYTE_KEY, BYTE_VALUE);
        jedis.sadd(SET_STRING_KEY, STRING_VALUE);

        jedis.zadd(ZSET_BYTE_KEY, 1, BYTE_VALUE);
        jedis.zadd(ZSET_STRING_KEY, 1, STRING_VALUE);

        jedis.hset(HASH_BYTE_KEY, HASH_FIELD_BYTE_KEY, BYTE_VALUE);
        jedis.hset(HASH_STRING_KEY, HASH_FIELD_STRING_KEY, STRING_VALUE);
    }

    public static Jedis getJedis() {
        return jedisPool.getResource();
    }

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

    private static class DummyBinaryPubSub extends BinaryJedisPubSub {

        @Override
        public void onMessage(byte[] channel, byte[] message) {
            System.out.println("Recieved message from channel [" + new String(channel) + "]. Message [" + new String(message) + "]");
        }

        @Override
        public void onPMessage(byte[] pattern, byte[] channel, byte[] message) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onSubscribe(byte[] channel, int subscribedChannels) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onUnsubscribe(byte[] channel, int subscribedChannels) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onPUnsubscribe(byte[] pattern, int subscribedChannels) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onPSubscribe(byte[] pattern, int subscribedChannels) {
            // TODO Auto-generated method stub

        }

    }

    private static class DummyPubSub extends JedisPubSub {

        @Override
        public void onMessage(String channel, String message) {
            System.out.println("Recieved message from channel [" + channel + "]. Message [" + message + "]");
        }

        @Override
        public void onPMessage(String pattern, String channel, String message) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onSubscribe(String channel, int subscribedChannels) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onUnsubscribe(String channel, int subscribedChannels) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onPUnsubscribe(String pattern, int subscribedChannels) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onPSubscribe(String pattern, int subscribedChannels) {
            // TODO Auto-generated method stub

        }

    }
}
