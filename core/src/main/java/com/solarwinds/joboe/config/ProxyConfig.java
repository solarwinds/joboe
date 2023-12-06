package com.solarwinds.joboe.config;

import lombok.Getter;

@Getter
public class ProxyConfig {
    private final String host;
    private final int port;
    private final String username;
    private final String password;

    public ProxyConfig(String host, int port, String username, String password) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
    }

}
