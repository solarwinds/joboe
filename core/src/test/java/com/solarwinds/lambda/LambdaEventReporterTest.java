package com.solarwinds.lambda;

import com.solarwinds.joboe.AtomicEventReporterStats;
import com.solarwinds.joboe.Event;
import com.solarwinds.joboe.rpc.Client;
import com.solarwinds.joboe.rpc.ClientException;
import com.solarwinds.joboe.rpc.Result;
import com.solarwinds.joboe.rpc.ResultCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LambdaEventReporterTest {

    @InjectMocks
    private LambdaEventReporter tested;

    @Mock
    private Client clientMock;

    @Mock
    private AtomicEventReporterStats atomicEventReporterStatsMock;

    @Mock
    private Future<Result> futureMock;

    @Mock
    private Event eventMock;

    @Test
    void verifyThatPostEventsIsInvokedOnClient() throws ClientException, ExecutionException, InterruptedException {
        when(clientMock.postEvents(anyList(), any())).thenReturn(futureMock);
        when(futureMock.get()).thenReturn(new Result(ResultCode.OK, "", ""));
        tested.send(eventMock);

        verify(clientMock).postEvents(anyList(), any());
    }


    @Test
    void verifyThatSentCountIsIncremented() throws ClientException, ExecutionException, InterruptedException {
        when(clientMock.postEvents(anyList(), any())).thenReturn(futureMock);
        when(futureMock.get()).thenReturn(new Result(ResultCode.OK, "", ""));
        tested.send(eventMock);

        verify(atomicEventReporterStatsMock).incrementSentCount(anyLong());
        verify(atomicEventReporterStatsMock, never()).incrementFailedCount(anyLong());
    }

    @Test
    void verifyThatFailedCountIsIncrementedOnErrorResult() throws ClientException, ExecutionException, InterruptedException {
        when(clientMock.postEvents(anyList(), any())).thenReturn(futureMock);
        when(futureMock.get()).thenReturn(new Result(ResultCode.INVALID_API_KEY, "", ""));
        tested.send(eventMock);

        verify(atomicEventReporterStatsMock).incrementFailedCount(anyLong());
        verify(atomicEventReporterStatsMock, never()).incrementSentCount(anyLong());
    }

    @Test
    void verifyThatProcessedCountIsIncrementedOnErrorResult() throws ClientException, ExecutionException, InterruptedException {
        when(clientMock.postEvents(anyList(), any())).thenReturn(futureMock);
        when(futureMock.get()).thenReturn(new Result(ResultCode.INVALID_API_KEY, "", ""));
        tested.send(eventMock);

        verify(atomicEventReporterStatsMock).incrementProcessedCount(anyLong());
    }

    @Test
    void verifyThatProcessedCountIsIncrementedOnResult() throws ClientException, ExecutionException, InterruptedException {
        when(clientMock.postEvents(anyList(), any())).thenReturn(futureMock);
        when(futureMock.get()).thenReturn(new Result(ResultCode.OK, "", ""));
        tested.send(eventMock);

        verify(atomicEventReporterStatsMock).incrementProcessedCount(anyLong());
    }

    @Test
    void verifyThatProcessedCountIsIncrementedException() throws ClientException {
        when(clientMock.postEvents(anyList(), any())).thenThrow(ClientException.class);
        tested.send(eventMock);

        verify(atomicEventReporterStatsMock).incrementProcessedCount(anyLong());
    }

    @Test
    void verifyThatFailedCountIsIncrementedException() throws ClientException {
        when(clientMock.postEvents(anyList(), any())).thenThrow(ClientException.class);
        tested.send(eventMock);

        verify(atomicEventReporterStatsMock).incrementFailedCount(anyLong());
    }

    @Test
    void testConsumeStats() {
        tested.consumeStats();
        verify(atomicEventReporterStatsMock).consumeStats();
    }

    @Test
    void testClose() {
        tested.close();
        verify(clientMock).close();
    }
}