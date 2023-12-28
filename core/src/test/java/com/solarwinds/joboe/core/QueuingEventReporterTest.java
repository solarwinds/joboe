package com.solarwinds.joboe.core;

import com.solarwinds.joboe.core.QueuingEventReporter;
import com.solarwinds.joboe.core.rpc.Client;
import com.solarwinds.joboe.core.rpc.Result;
import com.solarwinds.joboe.core.rpc.ResultCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.concurrent.Future;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QueuingEventReporterTest {

    @InjectMocks
    private QueuingEventReporter tested;

    @Mock
    private Client clientMock;

    @Mock
    private Future<Result> futureMock;

    @Test
    void testSynchronousSend() throws Exception {
        when(clientMock.postEvents(anyList(), any())).thenReturn(futureMock);
        when(futureMock.get()).thenReturn(new Result(ResultCode.OK, "",""));

        tested.synchronousSend(Collections.emptyList());
        verify(clientMock).postEvents(anyList(), any());
    }
}