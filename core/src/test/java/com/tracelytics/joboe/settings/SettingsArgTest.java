package com.tracelytics.joboe.settings;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.tracelytics.joboe.settings.SettingsArg.BooleanSettingsArg;
import com.tracelytics.joboe.settings.SettingsArg.DoubleSettingsArg;
import com.tracelytics.joboe.settings.SettingsArg.IntegerSettingsArg;
import junit.framework.TestCase;

public class SettingsArgTest extends TestCase {
    
    public void testDoubleSettingsArg() {
        SettingsArg<Double> arg = new DoubleSettingsArg("test-double");
        assertEquals("test-double", arg.getKey());
        ByteBuffer buffer = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putDouble(1.23);
        buffer.flip();
        assertEquals(1.23, arg.readValue(buffer));
    }
    
    public void testIntegerSettingsArg() {
        SettingsArg<Integer> arg = new IntegerSettingsArg("test-int");
        assertEquals("test-int", arg.getKey());
        ByteBuffer buffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(3);
        buffer.flip();
        assertEquals(3, (int) arg.readValue(buffer));
    }
    
    public void testBooleanSettingsArg() {
        SettingsArg<Boolean> arg = new BooleanSettingsArg("test-boolean");
        assertEquals("test-boolean", arg.getKey());
        ByteBuffer buffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN); 
        buffer.putInt(1); //boolean uses int in bytebuffer (binary)
        buffer.flip();
        assertEquals(true, (boolean) arg.readValue(buffer));
        
        buffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN); 
        buffer.putInt(0); //boolean uses int in bytebuffer (binary)
        buffer.flip();
        assertEquals(false, (boolean) arg.readValue(buffer));
    }

    public void testInvalidArg() {
        SettingsArg<Double> arg = new DoubleSettingsArg("test-invalid");
        assertEquals("test-invalid", arg.getKey());
        ByteBuffer buffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN); //4 bytes will trigger buffer underflow
        buffer.putInt(3);
        buffer.flip();
        assertEquals(null, arg.readValue(buffer)); //should just give null
    }

}
