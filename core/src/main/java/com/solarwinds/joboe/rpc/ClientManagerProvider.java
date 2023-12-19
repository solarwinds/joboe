package com.solarwinds.joboe.rpc;

import com.solarwinds.joboe.rpc.grpc.GrpcClientManager;
import com.solarwinds.logging.Logger;
import com.solarwinds.logging.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ClientManagerProvider {
    private ClientManagerProvider() {
    }

    private static final Logger logger = LoggerFactory.getLogger();
    private static final Map<Client.ClientType, RpcClientManager> registeredManagers = new HashMap<>();

    static {
        registeredManagers.put(Client.ClientType.GRPC, new GrpcClientManager());
    }

    public static Optional<RpcClientManager> getClientManager(Client.ClientType clientType) {
        logger.debug("Using " + clientType + " for rpc calls");
        return Optional.ofNullable(registeredManagers.get(clientType));
    }

    public static void closeAllManagers() {
        for (RpcClientManager clientManager : registeredManagers.values()) {
            clientManager.close();
        }
    }
}