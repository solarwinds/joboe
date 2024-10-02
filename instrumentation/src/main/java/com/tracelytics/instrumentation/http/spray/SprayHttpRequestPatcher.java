package com.tracelytics.instrumentation.http.spray;

import java.util.Arrays;
import java.util.List;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtConstructor;
import com.tracelytics.ext.javassist.CtField;
import com.tracelytics.ext.javassist.CtNewMethod;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.ConstructorMatcher;
import com.tracelytics.instrumentation.TvContextObjectAware;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.span.impl.Span;

/**
 * Tags the <code>spray.http.HttpRequest</code> to provide:
 * <ol>
 *  <li>Convenient method to clone a new HttpRequest with a header value (replace if already exists)</li>
 *  <li>Flags on whether certain events have been reported (to avoid duplicated instrumentation)</li>
 * <ol> 
 * @author pluk
 *
 */
public class SprayHttpRequestPatcher extends ClassInstrumentation {
    private static String CLASS_NAME = SprayHttpRequestPatcher.class.getName();
    private enum OpType { CTOR }
    @SuppressWarnings("unchecked")
    private static List<ConstructorMatcher<OpType>> constructorMatchers = Arrays.asList(new ConstructorMatcher<OpType>(new String[] {}, OpType.CTOR));
    
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
        throws Exception {
        cc.addField(CtField.make("private boolean tvSprayCanExitReported;", cc));
        cc.addMethod(CtNewMethod.make("public void setTvSprayCanExitReported(boolean sprayCanExitReported) { tvSprayCanExitReported = sprayCanExitReported; }", cc));
        cc.addMethod(CtNewMethod.make("public boolean getTvSprayCanExitReported() { return tvSprayCanExitReported; }", cc));
        
        String spanClassName = Span.class.getName();
        cc.addField(CtField.make("private " + spanClassName + " tvSprayRoutingSpan;", cc));
        cc.addMethod(CtNewMethod.make("public void setTvSprayRoutingSpan(" + spanClassName + " span) { tvSprayRoutingSpan = span; }", cc));
        cc.addMethod(CtNewMethod.make("public " + spanClassName + " getTvSprayRoutingSpan() { return tvSprayRoutingSpan; }", cc));
        
        //convenient method to create a new HttpRequest with a header. This is necessary for Spray as all the Http objects are immutable (so is the default List of Scala)
        cc.addMethod(CtNewMethod.make(
                  "public Object tvWithHeader(String headerName, String headerValue) {"
                + "    scala.collection.immutable.List headers = this.headers();"
                + "    spray.http.HttpHeader existingHeaderWithSameName = null; "
                + "    if (headers != null) {"
                + "        for (int i = 0; i < headers.length(); i++) {" //check whether a header with this name already exist, do not handle the case of multiple existing headers atm
                + "            spray.http.HttpHeader header = (spray.http.HttpHeader)headers.apply(i);"
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
                + "        scala.collection.immutable.List newHeaders = scala.collection.immutable.$colon$colon$.MODULE$.apply(new spray.http.HttpHeaders.RawHeader(headerName, headerValue), scala.collection.immutable.Nil$.MODULE$).$colon$colon$colon(existingHeaders);"
                + "        return this.withHeaders(newHeaders); "
                + "    } else {"
                + "        return null;"
                + "    }"
                + "}", cc));
        
        cc.addField(CtField.make("private boolean tvSslEncryption;", cc));
        cc.addMethod(CtNewMethod.make("public void tvSetSslEncryption(boolean sslEncryption) { tvSslEncryption = sslEncryption; }", cc));
        cc.addMethod(CtNewMethod.make("public boolean tvGetSslEncryption() { return tvSslEncryption; }", cc));
        
        cc.addMethod(CtNewMethod.make("public String tvUriScheme() { return uri().scheme(); }", cc));
        cc.addMethod(CtNewMethod.make("public int tvUriPort() { return uri().authority() != null ? uri().authority().port() : 0; }", cc));
        cc.addMethod(CtNewMethod.make("public String tvUriPath() { return uri().path() != null ? uri().path().toString() : null; }", cc));
        cc.addMethod(CtNewMethod.make("public String tvUriQuery() { return uri().query() != null ? uri().query().toString() : \"\"; }", cc));
        cc.addMethod(CtNewMethod.make("public String tvMethod() { return method().value(); }", cc));
        
        tagInterface(cc, SprayHttpRequest.class.getName());        
        
        addTvContextObjectAware(cc);
        
        for (CtConstructor constructor : findMatchingConstructors(cc, constructorMatchers).keySet()) {
            insertAfter(constructor, CLASS_NAME + ".tagContext(this);", true, false);
        }
        
        
        return true;
    }
    
    public static void tagContext(Object requestObject) {
        if (Context.getMetadata().isValid()) {
            ((TvContextObjectAware)requestObject).setTvContext(Context.getMetadata());
        }
    }
}