/*
 * ====================================================================
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */

package com.tracelytics.ext.apache.http.impl.conn;

import com.tracelytics.ext.apache.http.HttpResponse;
import com.tracelytics.ext.apache.http.HttpResponseFactory;
import com.tracelytics.ext.apache.http.annotation.Immutable;
import com.tracelytics.ext.apache.http.config.MessageConstraints;
import com.tracelytics.ext.apache.http.impl.DefaultHttpResponseFactory;
import com.tracelytics.ext.apache.http.io.HttpMessageParser;
import com.tracelytics.ext.apache.http.io.HttpMessageParserFactory;
import com.tracelytics.ext.apache.http.io.SessionInputBuffer;
import com.tracelytics.ext.apache.http.message.BasicLineParser;
import com.tracelytics.ext.apache.http.message.LineParser;

/**
 * Default factory for response message parsers.
 *
 * @since 4.3
 */
@Immutable
public class DefaultHttpResponseParserFactory implements HttpMessageParserFactory<HttpResponse> {

    public static final DefaultHttpResponseParserFactory INSTANCE = new DefaultHttpResponseParserFactory();

    private final LineParser lineParser;
    private final HttpResponseFactory responseFactory;

    public DefaultHttpResponseParserFactory(
            final LineParser lineParser,
            final HttpResponseFactory responseFactory) {
        super();
        this.lineParser = lineParser != null ? lineParser : BasicLineParser.INSTANCE;
        this.responseFactory = responseFactory != null ? responseFactory
                : DefaultHttpResponseFactory.INSTANCE;
    }

    public DefaultHttpResponseParserFactory(
            final HttpResponseFactory responseFactory) {
        this(null, responseFactory);
    }

    public DefaultHttpResponseParserFactory() {
        this(null, null);
    }

    @Override
    public HttpMessageParser<HttpResponse> create(final SessionInputBuffer buffer,
            final MessageConstraints constraints) {
        return new DefaultHttpResponseParser(buffer, lineParser, responseFactory, constraints);
    }

}
