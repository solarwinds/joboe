package com.solarwinds.joboe.rpc;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RpcClientManagerTest {
    private static final String TEST_SERVER_CERT_LOCATION = "src/test/java/com/solarwinds/joboe/rpc/test-collector-public.pem";


    @Test
    public void testValidEnvironmentVariables() throws Exception {
        RpcClientManager.init("unit-test-collector1:1234", TEST_SERVER_CERT_LOCATION);
        
        //verify the fields
        Field field;
        field = RpcClientManager.class.getDeclaredField("collectorHost");
        field.setAccessible(true);
        assertEquals("unit-test-collector1" , field.get(null));
        
        field = RpcClientManager.class.getDeclaredField("collectorPort");
        field.setAccessible(true);
        assertEquals(1234, field.get(null));
        
        field = RpcClientManager.class.getDeclaredField("collectorCertLocation");
        field.setAccessible(true);
        assertEquals(new File(TEST_SERVER_CERT_LOCATION).toURI().toURL(), field.get(null));


        RpcClientManager.init("unit-test-collector2", null);
        field = RpcClientManager.class.getDeclaredField("collectorHost");
        field.setAccessible(true);
        assertEquals("unit-test-collector2" , field.get(null));
        
        field = RpcClientManager.class.getDeclaredField("collectorPort");
        field.setAccessible(true);
        assertEquals(RpcClientManager.DEFAULT_PORT, field.get(null));

        RpcClientManager.init(null, null); //revert
    }

    @Test
    public void testInvalidEnvironmentVariables() throws Exception {
        RpcClientManager.init("unit-test-collector:not-a-number", null);
        
        //verify the fields, port number should fallback to default
        Field field;
        field = RpcClientManager.class.getDeclaredField("collectorHost");
        field.setAccessible(true);
        assertEquals("unit-test-collector" , field.get(null));
        
        field = RpcClientManager.class.getDeclaredField("collectorPort");
        field.setAccessible(true);
        assertEquals(RpcClientManager.DEFAULT_PORT, field.get(null));
        
        
        RpcClientManager.init(null, "not-found-location");
        field = RpcClientManager.class.getDeclaredField("collectorCertLocation");
        field.setAccessible(true);
        assertEquals(RpcClientManager.AO_DEFAULT_COLLECTER_CERT_LOCATION, field.get(null));
        
        RpcClientManager.init(null, null); //revert
    }

    @Test
    public void testDefaultEnviromentVariables() throws Exception {
        RpcClientManager.init(null, null);
        
        //verify the fields
        Field field;
        field = RpcClientManager.class.getDeclaredField("collectorHost");
        field.setAccessible(true);
        assertEquals(RpcClientManager.DEFAULT_HOST , field.get(null));
        
        field = RpcClientManager.class.getDeclaredField("collectorPort");
        field.setAccessible(true);
        assertEquals(RpcClientManager.DEFAULT_PORT, field.get(null));
        
        field = RpcClientManager.class.getDeclaredField("collectorCertLocation");
        field.setAccessible(true);
        assertEquals(RpcClientManager.AO_DEFAULT_COLLECTER_CERT_LOCATION, field.get(null));
    }
}
