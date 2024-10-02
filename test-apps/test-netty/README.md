Various standalone applications:

## Servers

1. proxy.TestHttpProxy - a http proxy using netty. It by default binds to port 8000 and forward requests to localhost:8080. Configurable via -D properties: `localPort`, `remoteHost`, `remotePort` 
2. SimpleServer - a http/1.1 server that binds to 8080, replies with value "dummy response". It can also be configured to serve https by using `-Dssl=true` (then binds to 8443)
3. multiplex.server.Http2Server - a http/2 server that uses mulitplexing, binds to 8080 by default, if `-Dssl-true`, then binds to 8443 with SSL
4. server.Http2Server - a http/2 server, binds to 8080 by default, if `-Dssl-true`, then binds to 8443 with SSL
5. tiles.Launcher - a http/2 server that serves images over http/2 and http/1.1 as tiles. It binds to both port 8080 (that serves http/1.1) and 8443 (that serves http/2)

If SSL is required, then run it apps JDK 8 (newer JDK, 1.8.0u144 failed use at least 1.8.0u231) and set JVM arg such as

```
-Xbootclasspath/p:C:\Users\Patson\.m2\repository\org\mortbay\jetty\alpn\alpn-boot\8.1.13.v20181017\alpn-boot-8.1.13.v20181017.jar
```

## Client
Most of the test cases are accessible via browser, there's also a standlone client `Http2Client`