package com.tracelytics.instrumentation;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map.Entry;
import java.util.Set;

import com.tracelytics.ext.ebson.BsonDocument;
import com.tracelytics.ext.ebson.BsonDocuments;
import com.tracelytics.joboe.BsonBufferException;
import com.tracelytics.joboe.Constants;
import com.tracelytics.joboe.Event;
import com.tracelytics.logging.Logger;
import com.tracelytics.logging.LoggerFactory;

public final class TestingUtil {
    private static final Logger logger = LoggerFactory.getLogger();
    private TestingUtil() {
    }
    
    /**
     * Event does not provide getInfo method, so we need this helper method to get the info in order to verify the result
     * @param event
     * @param key
     */
    public static Object getValueFromEvent(Event event, String key) throws BsonBufferException {
      ByteBuffer buffer =  ByteBuffer.allocate(Constants.MAX_EVENT_BUFFER_SIZE).order(ByteOrder.LITTLE_ENDIAN);
      buffer.put(event.toBytes());
      buffer.flip();
      
      BsonDocument doc = BsonDocuments.readFrom(buffer);
      return doc.get(key);
    }
    
    public static Set<Entry<String, Object>> getEntriesFromBytes(byte[] bytes) {
        ByteBuffer buffer =  ByteBuffer.allocate(Constants.MAX_EVENT_BUFFER_SIZE).order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(bytes);
        buffer.flip();
        
        BsonDocument doc = BsonDocuments.readFrom(buffer);
        return doc.entrySet();        
    }

    
    
//    public static <W> W createInstrumentedInstance(Class<W> rawClass, ClassInstrumentation instrumentation) throws NotFoundException, CannotCompileException, InstantiationException, IllegalAccessException {
//    
//    }
    

}
