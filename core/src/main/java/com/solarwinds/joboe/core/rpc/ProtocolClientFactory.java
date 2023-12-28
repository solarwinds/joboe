package com.solarwinds.joboe.core.rpc;

/**
 * Factory to build {@link ProtocolClient} instances
 * @param <C>   Actual type of the ProtocolClient
 */
public interface ProtocolClientFactory<C extends ProtocolClient> {
    C buildClient(String host, int port) throws ClientException;
}
