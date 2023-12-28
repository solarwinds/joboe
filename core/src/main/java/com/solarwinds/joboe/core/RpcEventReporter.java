package com.solarwinds.joboe.core;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

import com.solarwinds.joboe.core.rpc.*;
import com.solarwinds.joboe.core.rpc.Client.Status;

/**
 * A queuing event reporter that uses a RPC client to make synchronous outbound calls
 * @author pluk
 *
 */
@Deprecated
public class RpcEventReporter extends QueuingEventReporter {
    private final ClientLoggingCallback<Result> loggingCallback = new ClientLoggingCallback<Result>("send events");
    
    public RpcEventReporter(Client rpcClient) throws IOException {
        super(rpcClient);
    }
    
    @Override
    public Result synchronousSend(List<Event> events) throws InterruptedException, ExecutionException, ClientException  {
        return client.postEvents(events, loggingCallback).get(); //block until the client has finished the request
    }
    
    @Override
    public void close() {
        //quick shutdown to avoid excessive blocking if the client is not connected
        if (client.getStatus() != Status.OK) {
            logger.debug("RPC client is not OK. Shutting down the service now");
            executorService.shutdownNow(); 
        }
        super.close();
    }

    public static RpcEventReporter buildReporter(RpcClientManager.OperationType operationType) {
        try {
            return new RpcEventReporter(RpcClientManager.getClient(operationType));
        } catch (IOException | ClientException e) {
            logger.warn("Failed to initialize Event reporter for event: " + e.getMessage(), e);
            return null;
        }
    }
        
}
