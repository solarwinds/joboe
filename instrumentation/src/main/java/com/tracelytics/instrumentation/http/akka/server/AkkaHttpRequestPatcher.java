package com.tracelytics.instrumentation.http.akka.server;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtNewMethod;
import com.tracelytics.ext.javassist.NotFoundException;
import com.tracelytics.instrumentation.ClassInstrumentation;

/**
 * Patches `akka.http.javadsl.model.HttpRequest` to allow:
 * <ol>
 *  <li>Various convenient method to get values</li>
 *  <li>tvWithHeader method to return a new `HttpRequest` with a new header</li>
 * </ol>
 *
 * @author pluk
 *
 */
public class AkkaHttpRequestPatcher extends ClassInstrumentation {
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
            throws Exception {
        try {
            cc.getMethod("getHeader", "(Ljava/lang/String;)Ljava/util/Optional;"); //make sure the getHeader method has expected signature
            //convenient method to create a new RequestStart with a header. This is necessary as the akka http model objects are immutable (so is the default List of Scala)
            cc.addMethod(CtNewMethod.make(
                    "public String tvGetHeader(String key) {"
                            + "    java.util.Optional headerOption = getHeader(key);"
                            + "    return headerOption.isPresent() ? ((akka.http.javadsl.model.HttpHeader) headerOption.get()).value() : null;"
                            + "}", cc));

            //convenient method to create a new RequestStart with a header. This is necessary as the akka http model objects are immutable (so is the default List of Scala)
            cc.addMethod(CtNewMethod.make(
                    "public " + AkkaHttpRequest.class.getName() + " tvWithHeader(String headerName, String headerValue) {"
                            //+ "    scala.collection.immutable.Seq newHeaders = headers().$colon$plus(akka.http.javadsl.model.headers.RawHeader.create(headerName, headerValue), scala.collection.immutable.Seq$.MODULE$.canBuildFrom());"
                            + "    return (" + AkkaHttpRequest.class.getName() + ") addHeader(akka.http.javadsl.model.headers.RawHeader.create(headerName, headerValue)); "
                            + "}", cc));

            cc.addMethod(CtNewMethod.make("public String tvPath() { return getUri() != null && getUri().path() != null ? getUri().path().toString() : null; }", cc));
            cc.addMethod(CtNewMethod.make("public int tvPort() { return getUri() != null ? getUri().port() : -1; }", cc));
            cc.addMethod(CtNewMethod.make("public String tvHost() { return getUri() != null && getUri().host() != null  ? getUri().host().address() : null; }", cc));
            cc.addMethod(CtNewMethod.make("public String tvHttpMethod() { return method() != null ? method().name() : null; }", cc));
            cc.addMethod(CtNewMethod.make("public String tvScheme() { return getUri() != null ? getUri().scheme() : null; }", cc));
            cc.addMethod(CtNewMethod.make("public String tvQuery() { "
                            + "    if (getUri() != null) { "
                            + "        java.util.Optional queryStringOption = getUri().rawQueryString();  "
                            + "        return (String) queryStringOption.orElse(null);"
                            + "    }"
                            + "    return null;"
                            + "}"
                    , cc));

            tagInterface(cc, AkkaHttpRequest.class.getName());

            return true;
        } catch (NotFoundException e) {
            logger.debug("Not patching akka http request, probably running on unsupported older version");
            return false;
        }
    }
}