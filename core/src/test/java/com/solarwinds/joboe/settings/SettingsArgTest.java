package com.solarwinds.joboe.settings;

import com.solarwinds.joboe.settings.SettingsArg.BooleanSettingsArg;
import com.solarwinds.joboe.settings.SettingsArg.DoubleSettingsArg;
import com.solarwinds.joboe.settings.SettingsArg.IntegerSettingsArg;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class SettingsArgTest {

    @Test
    public void testDoubleSettingsArg() {
        SettingsArg<Double> arg = new DoubleSettingsArg("test-double");
        assertEquals("test-double", arg.getKey());
        ByteBuffer buffer = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putDouble(1.23);
        buffer.flip();
        assertEquals(1.23, arg.readValue(buffer));
    }

    @Test
    public void testIntegerSettingsArg() {
        SettingsArg<Integer> arg = new IntegerSettingsArg("test-int");
        assertEquals("test-int", arg.getKey());
        ByteBuffer buffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(3);
        buffer.flip();
        assertEquals(3, (int) arg.readValue(buffer));
    }

    @Test
    public void testBooleanSettingsArg() {
        SettingsArg<Boolean> arg = new BooleanSettingsArg("test-boolean");
        assertEquals("test-boolean", arg.getKey());
        ByteBuffer buffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN); 
        buffer.putInt(1); //boolean uses int in bytebuffer (binary)
        buffer.flip();
        assertEquals(true, arg.readValue(buffer));
        
        buffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN); 
        buffer.putInt(0); //boolean uses int in bytebuffer (binary)
        buffer.flip();
        assertEquals(false, arg.readValue(buffer));
    }

    @Test
    public void testInvalidArg() {
        SettingsArg<Double> arg = new DoubleSettingsArg("test-invalid");
        assertEquals("test-invalid", arg.getKey());
        ByteBuffer buffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN); //4 bytes will trigger buffer underflow
        buffer.putInt(3);
        buffer.flip();
        assertNull(arg.readValue(buffer)); //should just give null
    }

}
