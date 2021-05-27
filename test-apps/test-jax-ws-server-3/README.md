Simple testing endpoint that uses JAX-WS annotations (version 3+, Jakarta)

Package the war and deploy to a servlet server (non-jboss), this HAS to be deployed on servlet 5.0+ complaint server, for example Tomcat 10+

To test against the endpoint use client from project test-jax-ws-client (might have to modify in code the target endpoint), see the README.md of that project for more information

WSDL can be accessed via http://localhost:8080/test-jax-ws-server-3/hello?wsdl


