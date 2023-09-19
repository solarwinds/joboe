package com.tracelytics.joboe.rpc.grpc;

import com.solarwinds.trace.ingestion.proto.Collector;
import com.tracelytics.joboe.rpc.ClientException;
import com.tracelytics.joboe.rpc.ClientRecoverableException;
import com.tracelytics.joboe.rpc.ResultCode;
import com.tracelytics.joboe.rpc.SettingsResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junitpioneer.jupiter.ClearEnvironmentVariable;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileSettingsGrpcClientTest {

    private FileSettingsGrpcClient tested;

    @Mock
    private GrpcClient grpcClientMock;

    @Captor
    private ArgumentCaptor<Collector.SettingsResult> settingsResultArgumentCaptor;

    @Test
    @SetEnvironmentVariable(key = "LAMBDA_TASK_ROOT", value = "lambda eh!")
    @SetEnvironmentVariable(key = "AWS_LAMBDA_FUNCTION_NAME", value = "lambda Fn eh!")
    void throwExceptionWhenInLambdaAndFileDoesNotExist() {
        tested = new FileSettingsGrpcClient(grpcClientMock, "doesn't-exist");
        assertThrows(ClientRecoverableException.class, () -> tested.doGetSettings("fake key", "fake version"));
    }

    @Test
    @SetEnvironmentVariable(key = "LAMBDA_TASK_ROOT", value = "lambda eh!")
    @SetEnvironmentVariable(key = "AWS_LAMBDA_FUNCTION_NAME", value = "lambda Fn eh!")
    void returnSettingsWhenInLambdaAndFileDoesExist() throws ClientException {
        when(grpcClientMock.transformToLocalSettings(any()))
                .thenReturn(new SettingsResult(ResultCode.OK, "", "not to see here", Collections.emptyList()));
        tested = new FileSettingsGrpcClient(grpcClientMock, new File("src/test/resources/solarwinds-apm-settings-raw").getPath());
        tested.doGetSettings("fake key", "fake version");

        verify(grpcClientMock).transformToLocalSettings(settingsResultArgumentCaptor.capture());
        Collector.SettingsResult settingsResult = settingsResultArgumentCaptor.getValue();
        assertNotNull(settingsResult);
        assertEquals(120, settingsResult.getSettings(0).getTtl());
    }


    @Test
    @ClearEnvironmentVariable(key = "LAMBDA_TASK_ROOT")
    @ClearEnvironmentVariable(key = "AWS_LAMBDA_FUNCTION_NAME")
    void verifyThatGetSettingsIsDelegatedWhenNotInLambda() throws ClientException {
        when(grpcClientMock.doGetSettings(anyString(), anyString()))
                .thenReturn(new SettingsResult(ResultCode.OK, "", "not to see here", Collections.emptyList()));
        tested = new FileSettingsGrpcClient(grpcClientMock, "doesn't matter");
        tested.doGetSettings("fake key", "fake version");

        verify(grpcClientMock).doGetSettings("fake key", "fake version");
    }
}