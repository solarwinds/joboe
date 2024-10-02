package com.tracelytics.test.httpclient;

import com.tracelytics.test.httpclient.TestServlet.Target;

public class Endpoint {
    protected final Target target;

    public Endpoint(Target target) {
        this.target = target;
    }

    public Target getTarget() {
        return target;
    }
}

class FullUrlEndpoint extends Endpoint {
    FullUrlEndpoint(Target target) {
        super(target);
    }

    @Override
    public String toString() {
        return "[Full URL] " + target.method + " " + target.getFullUrl();
    }
}

class HttpHostEndpoint extends Endpoint {
    HttpHostEndpoint(Target target) {
        super(target);
    }

    @Override
    public String toString() {
        return "[Http Host URL] " + target.method + " " + target.getFullUrl();
    }
}
