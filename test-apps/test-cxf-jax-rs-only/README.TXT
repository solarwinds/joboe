This is an testing application for Apache CXF which uses only cxf-bundle-jaxrs. This is to test on a reported issue on https://tickets.appneta.com/helpdesk/tickets/2721 that below warning was observed

Sep 19, 2014 05:08:20.413 PM WARNING [Tracelytics] Unable to instrument class [org/apache/cxf/jaxrs/client/FrontendClientAdapter], instrumentation is skipped : [source error] no such field: org/apache/cxf/binding/soap/SoapBindingConstants/SOAP_ACTION
com.tracelytics.a.d.a: [source error] no such field: org/apache/cxf/binding/soap/SoapBindingConstants/SOAP_ACTION
        at com.tracelytics.a.d.h.a(SourceFile:766)
        at com.tracelytics.instrumentation.ClassInstrumentation.a(SourceFile:473)
        at com.tracelytics.instrumentation.ClassInstrumentation.a(SourceFile:464)
        at com.tracelytics.instrumentation.http.ws.CxfEndpointClientInstrumentation.applyInstrumentation(SourceFile:30)
        
More details can be found in https://github.com/tracelytics/joboe/issues/319

Links below would trigger web-service call to below:
http://<host>/test-cxf-jax-rs-only/ -sample local REST server (REST, see test-apps/test-rest-server)
