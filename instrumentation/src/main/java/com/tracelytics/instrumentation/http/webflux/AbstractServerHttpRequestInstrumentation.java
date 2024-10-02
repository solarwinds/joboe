package com.tracelytics.instrumentation.http.webflux;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.CtConstructor;
import com.tracelytics.ext.javassist.NotFoundException;
import com.tracelytics.instrumentation.ClassInstrumentation;
import com.tracelytics.instrumentation.ConstructorMatcher;
import com.tracelytics.joboe.span.impl.ScopeManager;
import com.tracelytics.joboe.span.impl.Span;
import com.tracelytics.joboe.span.impl.SpanDictionary;
import com.tracelytics.logging.Logger;
import com.tracelytics.logging.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * This instrumentation inserts the span id into the HttpHeaders when the webflux app runs on a servlet container, e.g., Jetty or Tomcat
 * (vs non servlet container such as netty or undertow).
 *
 * This is necessary as the webflux handler might be running on a different thread (ie default thread local context would not work), 
 * hence we would need to look up the span created from the request header.
 *
 * Take note that we cannot use ServletWithSpanContextInstrumentation to insert the span id, as the HttpHeaders is created before the patch in 
 * ServletWithSpanContextInstrumentation is called.
 */
public class AbstractServerHttpRequestInstrumentation extends ClassInstrumentation {
    public static final String CLASS_NAME = AbstractServerHttpRequestInstrumentation.class.getName();
    protected static final Logger logger = LoggerFactory.getLogger();

    private boolean writableHttpHeadersExists = true;
    private enum Type { HTTP_HEADERS, MULTI_VALUE_MAP }

    @SuppressWarnings("unchecked")
    private static List<ConstructorMatcher<Object>> constructorMatchers = Arrays.asList(
            new ConstructorMatcher<Object>(new String[] { "java.net.URI", "java.lang.String", "org.springframework.http.HttpHeaders"}, Type.HTTP_HEADERS),
            new ConstructorMatcher<Object>(new String[] { "java.net.URI", "java.lang.String", "org.springframework.util.MultiValueMap"}, Type.MULTI_VALUE_MAP)
    );

    @Override
    public boolean applyInstrumentation(CtClass cc, String className, byte[] classBytes)
            throws Exception {
        CtClass httpHeaderClass = classPool.get("org.springframework.http.HttpHeaders");
        try {
            if (httpHeaderClass.getMethod("writableHttpHeaders",
                    "(Lorg/springframework/http/HttpHeaders;)Lorg/springframework/http/HttpHeaders;") != null) {
                writableHttpHeadersExists = true;
            }
        } catch (NotFoundException e) {
            logger.debug("Method org.springframework.http.HttpHeaders#writebleHttpHeaders is not found.");
            writableHttpHeadersExists = false;
        }

        for (Map.Entry<CtConstructor, Object> entry : findMatchingConstructors(cc, constructorMatchers).entrySet()) {
            CtConstructor constructor = entry.getKey();

            String getWritableHttpHeaders;
            if (entry.getValue() == Type.HTTP_HEADERS) {
                getWritableHttpHeaders = "$3 = org.springframework.http.HttpHeaders.writableHttpHeaders($3);";
                if (!writableHttpHeadersExists) {
                    getWritableHttpHeaders = "";
                }
            } else {
                getWritableHttpHeaders = "";
            }

            insertBefore(constructor,
                    "String key = \"" + ClassInstrumentation.X_SPAN_KEY + "\";"
                            + "String value = " + CLASS_NAME + ".getSpanId();"
                            + "if (value != null && $3 != null) {"
                            + getWritableHttpHeaders
                            + "    $3.add(key, value);"
                            + "}", false);
        }

        return true;
    }

    public static String getSpanId() {
        Span currentSpan = ScopeManager.INSTANCE.activeSpan();
        if (currentSpan != null) {
            final long spanKey = SpanDictionary.setSpan(currentSpan);
            return String.valueOf(spanKey);
        }
        return null;
    }
}
