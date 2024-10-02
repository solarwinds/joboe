package com.tracelytics.joboe;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.tracelytics.logging.Logger;
import com.tracelytics.logging.LoggerFactory;

/**
 * Mock reporter similar to {@link TestReporter}, only that this one does not work in thread local manner
 * @author pluk
 *
 */
class NonThreadLocalTestReporter extends TestReporter {
    private Logger logger = LoggerFactory.getLogger();
    
    private List<byte[]> byteBufferList = new LinkedList<byte[]>();
            
    
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
        return ((LinkedList<byte[]>)byteBufferList).getLast();
    }
   
    @Override
    public synchronized List<byte[]> getBufList() {
        return new ArrayList<byte[]>(byteBufferList); //return a clone
    }
} 

