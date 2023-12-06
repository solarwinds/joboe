package com.solarwinds.util;


import com.solarwinds.joboe.BsonBufferException;
import com.solarwinds.joboe.ebson.BsonDocument;
import com.solarwinds.joboe.ebson.BsonDocuments;
import com.solarwinds.joboe.ebson.BsonToken;
import com.solarwinds.joboe.ebson.BsonWriter;

import java.nio.Buffer;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map;

public class BsonUtils {
    private BsonUtils() {
    }

    public static ByteBuffer convertMapToBson(Map<String, Object> map, int bufferSize, int maxBufferSize) throws BsonBufferException {
        BsonDocument doc = BsonDocuments.copyOf(map);
        BsonWriter writer = BsonToken.DOCUMENT.writer();
        ByteBuffer buffer = ByteBuffer.allocate(bufferSize).order(ByteOrder.LITTLE_ENDIAN);

        RuntimeException bufferException;
        try {
            writer.writeTo(buffer, doc);
            ((Buffer) buffer).flip();  //cast for JDK 8- runtime compatibility
            return buffer;
        } catch (BufferOverflowException e) { //cannot use multi-catch due to 1.6 compatibility
            bufferException = e;
        } catch (IllegalArgumentException e) { //IllegalArumgnentException could be thrown from BsonWriter.DOCUMENT.writeto if buffer position is greater than limit - 4
            bufferException = e;
        }

        if (bufferException != null) {
            ((Buffer) buffer).clear();  //cast for JDK 8- runtime compatibility
            if (bufferSize * 2 <= maxBufferSize) {
                return convertMapToBson(map, bufferSize * 2, maxBufferSize);
            } else {
                throw new BsonBufferException(bufferException);
            }
        } else {
            //shouldn't really run to here base on current logic flow, but just to play safe
            return buffer;
        }
    }

    public static Map<String, Object> convertBsonToMap(ByteBuffer byteBuffer) {
        Object o = BsonToken.DOCUMENT.reader().readFrom(byteBuffer);

        return (Map<String, Object>) o;
    }

}
