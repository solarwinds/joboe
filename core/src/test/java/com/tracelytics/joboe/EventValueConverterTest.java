package com.tracelytics.joboe;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.URL;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Converts object into values compatible to BSON
 * @author pluk
 *
 */
public class EventValueConverterTest {
    private static final int MAX_VALUE_LENGTH = 100;
    private EventValueConverter converter = new EventValueConverter(MAX_VALUE_LENGTH);
    Object o;
    int objectId;

    @Test
    public void testSimpleTypeBooleans() {
        assertEquals(true, converter.convertToEventValue(true));
        assertEquals(Boolean.TRUE, converter.convertToEventValue(Boolean.TRUE));
    }

    @Test
    public void testSimpleTypeDoubles() {
        assertEquals((double) 0, converter.convertToEventValue((double) 0));
        assertEquals(new Double(0), converter.convertToEventValue(new Double(0)));
    }

    @Test
    public void testSimpleTypeIntegers() {
        assertEquals(0, converter.convertToEventValue(0));
        assertEquals(new Integer(0), converter.convertToEventValue(new Integer(0)));
    }

    @Test
    public void testSimpleTypeLongs() {
        assertEquals((long) 0, converter.convertToEventValue((long) 0));
        assertEquals(new Long(0), converter.convertToEventValue(new Long(0)));
    }

    @Test
    public void testSimpleTypeEmptyString() {
        assertEquals("", converter.convertToEventValue(""));
    }

    @Test
    public void testSpecialTypeDate() throws Exception {
        Date date = new Date();
        assertEquals(date, converter.convertToEventValue(date));
    }

    @Test
    public void testSpecialTypeTime() throws Exception {
        Time time = new Time(System.currentTimeMillis());
        assertEquals(time, converter.convertToEventValue(time));
    }

    @Test
    public void testSpecialTypeBigDecimal() throws Exception {
        assertEquals((double) 0, converter.convertToEventValue(BigDecimal.ZERO));
    }

    @Test
    public void testSpecialTypeBigInteger() throws Exception {
        assertEquals((long) 0, converter.convertToEventValue(BigInteger.ZERO));
    }

    @Test
    public void testSpecialTypeFloat() throws Exception {
        assertEquals((double) 0, converter.convertToEventValue((float) 0));
        assertEquals((double) 0, converter.convertToEventValue(new Float(0)));
    }

    @Test
    public void testSpecialTypeShort() throws Exception {
        assertEquals(0, converter.convertToEventValue((short) 0));
        assertEquals(0, converter.convertToEventValue(new Short((short) 0)));
    }

    @Test
    public void testSpecialTypeByte() throws Exception {
        assertEquals(0, converter.convertToEventValue((byte) 0));
        assertEquals(0, converter.convertToEventValue(new Byte((byte)0)));
    }

    @Test
    public void testSpecialTypeChar() throws Exception {
        assertEquals("a", converter.convertToEventValue('a'));
        assertEquals("a", converter.convertToEventValue(new Character('a')));
    }

    @Test
    public void testSpecialTypeLongString() throws Exception {
        String rawString = new String(new byte[10000]);
        int truncateCount = rawString.length() - MAX_VALUE_LENGTH;
        String finalString = rawString.substring(0, MAX_VALUE_LENGTH) +  "...(" + truncateCount + " characters truncated)";
        assertEquals(finalString, converter.convertToEventValue(rawString));
    }

    @Test
    public void testSpecialTypeByteArray() throws Exception {
        o = new byte[0];
        objectId = System.identityHashCode(o);
        assertEquals("(Byte array 0 Bytes) id [" + objectId + "]", converter.convertToEventValue(o));
    }

    @Test
    public void testSpecialTypeURL() throws Exception {
        o = new URL("http://www.google.com");
        assertEquals(o.toString(), converter.convertToEventValue(o));
    }

    @Test
    public void testSpecialTypeIPAddress() throws Exception {
        o = InetAddress.getByAddress(new byte[4]);
        assertEquals(o.toString(), converter.convertToEventValue(o));
        o = InetAddress.getByAddress(new byte[16]);
        assertEquals(o.toString(), converter.convertToEventValue(o));
    }

    @Test
    public void testSpecialTypeArrayList() throws Exception {
        o = new ArrayList<Object>();
        objectId = System.identityHashCode(o);
        assertEquals("(Collection of class [" + ArrayList.class.getName() + "] with 0 Elements) id [" + objectId + "]", converter.convertToEventValue(o));
    }

    @Test
    public void testSpecialTypeHashMap() throws Exception {
        o = new HashMap<Object, Object>();
        objectId = System.identityHashCode(o);
        assertEquals("(Map of class [" + HashMap.class.getName() + "] with 0 Elements) id [" + objectId + "]", converter.convertToEventValue(o));
    }

    @Test
    public void testSpecialTypeUUID() throws Exception {
        o = UUID.randomUUID();
        assertEquals(o.toString(), converter.convertToEventValue(o));
    }

    @Test
    public void testSpecialTypeObject() throws Exception {        
        o = new Object();
        objectId = System.identityHashCode(o);
        assertEquals("(" + Object.class.getName() + ") id [" + objectId + "]", converter.convertToEventValue(o));

    }

}
