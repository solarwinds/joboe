package com.solarwinds.metrics.histogram;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class HdrHistrogramAdapterTest {
    private static final long MAX_VALUE = 60L * 60 * 1000 * 1000;

    @Test
    public void testEncode() throws Exception {
        HdrHistogramAdapter testHistogram = new HdrHistogramAdapter(MAX_VALUE, HistogramFactory.NUMBER_OF_SIGNIFICANT_VALUE_DIGITS);
        
        testHistogram.recordValue(3335);
        testHistogram.recordValue(3108);
        testHistogram.recordValue(2638);
        testHistogram.recordValue(2567);
        testHistogram.recordValue(2470);
        
        byte[] encoded = testHistogram.encodeBase64();
        
        String expectedEncodedString = "HISTFAAAAC14nJNpmSzMwMDAywABzFCaEURcm7yEwf4DRGCpMlMskyvTZWam+4xMAKW6B74="; //extracted from encode call of above values from c-lib
        assertEquals(expectedEncodedString, new String(encoded));
        
        ByteBuffer buffer = ByteBuffer.allocate(encoded.length);
        buffer.put(Base64.getDecoder().decode(encoded));
        buffer.rewind();
        
        com.solarwinds.metrics.hdrHistogram.Histogram readHistogram = com.solarwinds.metrics.hdrHistogram.Histogram.decodeFromCompressedByteBuffer(buffer, 0);
        
        assertEquals(testHistogram.getUnderlyingHistogram(), readHistogram);
    }

    @Test
    public void testRange() throws Exception {
        
        HdrHistogramAdapter testHistogram = new HdrHistogramAdapter(MAX_VALUE, HistogramFactory.NUMBER_OF_SIGNIFICANT_VALUE_DIGITS);
        
        testHistogram.recordValue(111); //ok
        testHistogram.recordValue(0); //MIN_VALUE ok
        testHistogram.recordValue(MAX_VALUE); //MAX_VALUE OK
        
        try {
            testHistogram.recordValue(-1); //negative value not ok
            fail("Expect exception " + HistogramOutOfRangeException.class.getName() + " to be thrown");
        } catch (HistogramOutOfRangeException e) {
            //expected
        }
        
        try {
            testHistogram.recordValue(MAX_VALUE + 1); //greater than MAX_VALUE not ok
            fail("Expect exception " + HistogramOutOfRangeException.class.getName() + " to be thrown");
        } catch (HistogramOutOfRangeException e) {
            //expected
        }
        
    }
}
