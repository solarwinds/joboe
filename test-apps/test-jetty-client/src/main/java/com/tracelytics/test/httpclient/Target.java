package com.tracelytics.test.httpclient;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

public abstract class Target {
    protected Method method;

    public Target(Method method) {
        this.method = method;
    }

    public abstract String getFullUrl(HttpServletRequest request);

    public Method getMethod() {
        return method;
    }

    enum Method { GET, POST, PUT, DELETE, HEAD }
}

class AbsoluteTarget extends Target {
    private String protocol;
    private String host;
    private Integer port;
    private String uri;


    public AbsoluteTarget(String protocol, String host, Integer port, String uri, Method method) {
        super(method);
        this.protocol = protocol;
        this.host = host;
        this.port = port;
        this.uri = uri;

    }

    @Override
    public String getFullUrl(HttpServletRequest servletRequest) {
        return protocol + "://" + host + (port != null ? (":" + port) : "") + uri;
    }

    @Override
    public String toString() {
        return "AbsoluteTarget{" +
                "method=" + method +
                ", protocol='" + protocol + '\'' +
                ", host='" + host + '\'' +
                ", port=" + port +
                ", uri='" + uri + '\'' +
                '}';
    }
}

class RelativeTarget extends Target {
    private final String relativePath;

    public RelativeTarget(String relativePath, Method method) {
        super(method);
        this.relativePath = relativePath;
    }

    @Override
    public String getFullUrl(HttpServletRequest request) {
        return request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath() + "/" + relativePath;
    }

    @Override
    public String toString() {
        return "RelativeTarget{" +
                "method=" + method +
                ", relativePath='" + relativePath + '\'' +
                '}';
    }
}


