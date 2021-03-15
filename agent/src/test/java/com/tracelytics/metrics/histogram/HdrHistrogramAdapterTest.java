package com.tracelytics.metrics.histogram;

import java.nio.ByteBuffer;

import com.tracelytics.ext.base64.Base64;
import com.tracelytics.joboe.JoboeTest;
import com.tracelytics.metrics.histogram.HdrHistogramAdapter;
import com.tracelytics.metrics.histogram.HistogramFactory;
import com.tracelytics.metrics.histogram.HistogramOutOfRangeException;

public class HdrHistrogramAdapterTest extends JoboeTest {
    private static long MAX_VALUE = 60L * 60 * 1000 * 1000;
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
        buffer.put(Base64.decode(encoded));
        buffer.rewind();
        
        com.tracelytics.ext.hdrHistogram.Histogram readHistogram = com.tracelytics.ext.hdrHistogram.Histogram.decodeFromCompressedByteBuffer(buffer, 0);
        
        assertEquals(testHistogram.getUnderlyingHistogram(), readHistogram);
    }
    
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
