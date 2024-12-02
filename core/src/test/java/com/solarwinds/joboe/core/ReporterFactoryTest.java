package com.solarwinds.joboe.core;

import com.solarwinds.joboe.core.rpc.Client;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.net.InetAddress;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
public class ReporterFactoryTest {

    private static ReporterFactory tested;

    @Mock
    private Client clientMock;

    @BeforeAll
    static void setup() {
        tested = ReporterFactory.getInstance();
    }

    @Test
    public void testbuildNonDefaultUdpReporter() throws Exception {
        UDPReporter reporter = tested.createUdpReporter("localhost", 9999);

        Field addressField = reporter.getClass().getDeclaredField("addr");
        addressField.setAccessible(true);

        Field portField = reporter.getClass().getDeclaredField("port");
        portField.setAccessible(true);

        InetAddress address = (InetAddress) addressField.get(reporter);
        assertEquals(InetAddress.getByName("localhost"), address);
        assertEquals(9999, portField.get(reporter));
    }

    @Test
    void testCreateQueuingEventReporter() {
        assertNotNull(tested.createQueuingEventReporter(clientMock));
    }
}
