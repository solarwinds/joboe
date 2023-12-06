package com.solarwinds.joboe;

import com.solarwinds.joboe.rpc.Client;
import com.solarwinds.joboe.rpc.HostType;
import com.solarwinds.lambda.LambdaEventReporter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.net.InetAddress;

import static org.junit.jupiter.api.Assertions.*;

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
    public void testbuildDefaultUdpReporter() throws Exception {
        UDPReporter reporter = tested.createUdpReporter();

        Field addressField = reporter.getClass().getDeclaredField("addr");
        addressField.setAccessible(true);

        Field portField = reporter.getClass().getDeclaredField("port");
        portField.setAccessible(true);

        InetAddress address = (InetAddress) addressField.get(reporter);
        assertEquals(InetAddress.getByName(Constants.XTR_UDP_HOST), address);
        assertEquals(Constants.XTR_UDP_PORT, portField.get(reporter));
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
    void testCreateLambdaReporter() {
        assertNotNull(tested.createLambdaReporter(clientMock));
    }

    @Test
    void testCreateQueuingEventReporter() {
        assertNotNull(tested.createQueuingEventReporter(clientMock));
    }

    @Test
    void testCreateHostTypeReporterForPersistentHost() {
        assertTrue(tested.createHostTypeReporter(clientMock, HostType.PERSISTENT) instanceof QueuingEventReporter);
    }

    @Test
    void testCreateHostTypeReporterForLambda() {
        assertTrue(tested.createHostTypeReporter(clientMock, HostType.AWS_LAMBDA) instanceof LambdaEventReporter);
    }
}
