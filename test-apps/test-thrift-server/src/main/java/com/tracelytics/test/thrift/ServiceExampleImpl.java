package com.tracelytics.test.thrift;

import java.nio.ByteBuffer;

import org.apache.thrift.TException;

public class ServiceExampleImpl implements ServiceExample.Iface {
    private final ByteBuffer BYTE_CONTENT = ByteBuffer.wrap(new byte[] { 3, 1, 4 });
    @Override
    public BeanExample getBean(int anArg, String anOther) throws TException {
//        long start = System.nanoTime();
        
        BeanExample result = new BeanExample(true, (byte) 2, (short) 3, 4, 5, 6.0,
                               "OK", BYTE_CONTENT);
        
//        System.out.println("used: " + (System.nanoTime() - start) + "ns");
        
        return result;
    }
    @Override
    public void testOneWay() throws TException {
        // TODO Auto-generated method stub
    }
}