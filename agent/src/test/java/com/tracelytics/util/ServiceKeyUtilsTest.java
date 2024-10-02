package com.tracelytics.util;

import java.util.Arrays;

import junit.framework.TestCase;

public class ServiceKeyUtilsTest extends TestCase {
    public void testGetServiceName() {
        assertEquals("good-service", ServiceKeyUtils.getServiceName("123:good-service"));
        assertEquals("service-name-with:::", ServiceKeyUtils.getServiceName("123:service-name-with:::"));
        assertEquals("", ServiceKeyUtils.getServiceName("123:"));
        assertEquals(null, ServiceKeyUtils.getServiceName(""));
        assertEquals(null, ServiceKeyUtils.getServiceName("123"));
    }
    
    public void testMaskServiceKey() {
        assertEquals("ec3d********************************************************5468:good-service", ServiceKeyUtils.maskServiceKey("ec3d1519afe2f54474d3d3cc2c9af0aff9f6e939c0d6302d768d808378025468:good-service"));
        assertEquals("ec3d********************************************************5468:", ServiceKeyUtils.maskServiceKey("ec3d1519afe2f54474d3d3cc2c9af0aff9f6e939c0d6302d768d808378025468:"));
        assertEquals("ec3d********************************************************5468", ServiceKeyUtils.maskServiceKey("ec3d1519afe2f54474d3d3cc2c9af0aff9f6e939c0d6302d768d808378025468"));
        assertEquals("1234*6789:masked", ServiceKeyUtils.maskServiceKey("123456789:masked"));
        assertEquals("12345678:too-short", ServiceKeyUtils.maskServiceKey("12345678:too-short"));
                
    }
    
    public void testTransformServiceKey() {
        assertEquals("123:good-service", ServiceKeyUtils.transformServiceKey("123:good-service"));
        assertEquals("123:service-name-with:::colons", ServiceKeyUtils.transformServiceKey("123:service-name-with:::colons"));
        assertEquals("123:--service-name-with-spaces--", ServiceKeyUtils.transformServiceKey("123:  service name with spaces  "));
        assertEquals("123:", ServiceKeyUtils.transformServiceKey("123:@#$%^&*()"));
        assertEquals("123:.:_-", ServiceKeyUtils.transformServiceKey("123:.:_-"));
        assertEquals("123:cap", ServiceKeyUtils.transformServiceKey("123:CAP"));
        assertEquals("123", ServiceKeyUtils.transformServiceKey("123"));
         
        char[] longServiceNameArray = new char[ServiceKeyUtils.SERVICE_NAME_MAX_LENGTH + 1]; //1 char too long
        Arrays.fill(longServiceNameArray, '1');
        String longServiceKey = "123:" + new String(longServiceNameArray);
        
        assertEquals(longServiceKey.substring(0, longServiceKey.length() - 1), ServiceKeyUtils.transformServiceKey(longServiceKey));
        
        assertEquals("", ServiceKeyUtils.transformServiceKey(""));
    }
}
