package com.tracelytics.instrumentation.http.akka.server;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtNewMethod;
import com.tracelytics.ext.javassist.NotFoundException;
import com.tracelytics.instrumentation.ClassInstrumentation;

/**
 * Patches the `akka.http.impl.engine.parsing.ParserOutput$RequestStart` to provide:
 * <ol>
 *  <li>Convenient method to clone a new RequestStart instance with a header value (replace if already exists)</li>
 *  <li>Methods to expose various field values</li>
 * </ol>
 *
 * @author pluk
 *
 */
public class AkkaHttpRequestStartPatcher extends ClassInstrumentation {
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
            throws Exception {
        if (!isSupportedVersion(cc)) {
            logger.debug("Not a supported version of Akka Http server, do not patch the Akka RequestStart object");
            return false;
        }

        //convenient method to create a new RequestStart with a header. This is necessary as the akka http model objects are immutable (so is the default List of Scala)
        cc.addMethod(CtNewMethod.make(
                "public " + AkkaHttpRequestStart.class.getName() + " tvWithHeader(String headerName, String headerValue) {"
                        + "    scala.collection.immutable.List headers = this.headers();"
                        + "    akka.http.javadsl.model.HttpHeader existingHeaderWithSameName = null; "
                        + "    if (headers != null) {"
                        + "        for (int i = 0; i < headers.length(); i++) {" //check whether a header with this name already exist, do not handle the case of multiple existing headers atm
                        + "            akka.http.javadsl.model.HttpHeader header = (akka.http.javadsl.model.HttpHeader)headers.apply(i);"
                        + "            if (header != null && header.is(headerName.toLowerCase())) {"
                        + "                existingHeaderWithSameName = header;"
                        + "                break;"
                        + "            }"
                        + "        }"
                        + "        scala.collection.immutable.List existingHeaders;"
                        + "        if (existingHeaderWithSameName == null) {" //does not have an existing header with same name, can simply add a new one
                        + "            existingHeaders = headers;"
                        + "        } else {" //have to remove the existing header first
                        + "            existingHeaders = (scala.collection.immutable.List)headers.diff(scala.collection.immutable.$colon$colon$.MODULE$.apply(existingHeaderWithSameName, scala.collection.immutable.Nil$.MODULE$));"
                        + "        }"
                        + "        scala.collection.immutable.List newHeaders = scala.collection.immutable.$colon$colon$.MODULE$.apply(akka.http.javadsl.model.headers.RawHeader.create(headerName, headerValue), scala.collection.immutable.Nil$.MODULE$).$colon$colon$colon(existingHeaders);"
                        + "        return (" + AkkaHttpRequestStart.class.getName() + ") this.copy(method(), uri(), protocol(), newHeaders, createEntity(), expect100Continue(), closeRequested()); "
                        + "    } else {"
                        + "        return null;"
                        + "    }"
                        + "}", cc));

        cc.addMethod(CtNewMethod.make(
                "public String tvGetHeader(String key) {"
                        + "    if (headers() != null) { "
                        + "        for (int i = 0; i < headers().length(); i++) {"
                        + "            akka.http.scaladsl.model.HttpHeader header = (akka.http.scaladsl.model.HttpHeader)headers().apply(i);"
                        + "            if (key.equalsIgnoreCase(header.name())) {"
                        + "                return header.value();"
                        + "            }"
                        + "        }"
                        + "    }"
                        + "    return null;"
                        + "}", cc));

        cc.addMethod(CtNewMethod.make("public String tvUriPath() { return uri() != null && uri().path() != null ? uri().path().toString() : null; }", cc));
        cc.addMethod(CtNewMethod.make("public int tvUriPort() { return uri() != null && uri().authority() != null ? uri().authority().port() : -1; }", cc));
        cc.addMethod(CtNewMethod.make("public String tvUriHost() { return uri() != null && uri().authority() != null && uri().authority().host() != null  ? uri().authority().host().address() : null; }", cc));
        cc.addMethod(CtNewMethod.make("public String tvHttpMethod() { return method() != null ? method().name() : null; }", cc));
        cc.addMethod(CtNewMethod.make("public String tvScheme() { return uri() != null ? uri().scheme() : null; }", cc));
        cc.addMethod(CtNewMethod.make("public String tvQuery() { "
                        + "    if (uri() != null) { "
                        + "        scala.Option queryStringOption = uri().queryString(uri().queryString$default$1());  "
                        + "        return queryStringOption.isDefined() ? (String)queryStringOption.get() : null;"
                        + "    }"
                        + "    return null;"
                        + "}"
                , cc));




        tagInterface(cc, AkkaHttpRequestStart.class.getName());
        return true;
    }

    private static boolean isSupportedVersion(CtClass cc) {
        try {
            cc.getDeclaredMethod("expect100Continue");
            return true;
        } catch (NotFoundException e) {
            logger.debug("Cannot find the method expect100Continue, not a supported version of Akka http " + e.getMessage());
            return false;
        }
    }
}