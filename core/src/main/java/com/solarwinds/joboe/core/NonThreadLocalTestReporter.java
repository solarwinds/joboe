package com.solarwinds.joboe.core;

import com.solarwinds.joboe.logging.Logger;
import com.solarwinds.joboe.logging.LoggerFactory;

import java.util.Deque;
import java.util.LinkedList;

/**
 * Mock reporter similar to {@link TestReporter}, only that this one does not work in thread local manner
 * @author pluk
 *
 */
class NonThreadLocalTestReporter extends TestReporter {
    private final Logger logger = LoggerFactory.getLogger();
    
    private final Deque<byte[]> byteBufferList = new LinkedList<byte[]>();
            
    
    NonThreadLocalTestReporter() {
    }
    
    @Override
    public synchronized void reset() {
        byteBufferList.clear();
    }

    @Override
    public synchronized void send(Event event) {
        try {
            byte[] buf = event.toBytes();
            logger.debug("Sent " + buf.length + " bytes");
            byteBufferList.add(buf);
        } catch (BsonBufferException e) {
            logger.error("Failed to send events : " + e.getMessage(), e);
        }
    }
        
    @Override
    public synchronized byte[] getLastSent() {
        return byteBufferList.getLast();
    }
   
    @Override
    public synchronized Deque<byte[]> getBufList() {
        return new LinkedList<>(byteBufferList); //return a clone
    }
} 

