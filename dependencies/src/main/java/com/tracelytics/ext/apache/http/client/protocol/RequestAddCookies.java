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

package com.tracelytics.ext.apache.http.client.protocol;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.tracelytics.ext.apache.http.Header;
import com.tracelytics.ext.apache.http.HttpException;
import com.tracelytics.ext.apache.http.HttpHost;
import com.tracelytics.ext.apache.http.HttpRequest;
import com.tracelytics.ext.apache.http.HttpRequestInterceptor;
import com.tracelytics.ext.apache.http.annotation.Immutable;
import com.tracelytics.ext.apache.http.client.CookieStore;
import com.tracelytics.ext.apache.http.client.config.CookieSpecs;
import com.tracelytics.ext.apache.http.client.config.RequestConfig;
import com.tracelytics.ext.apache.http.client.methods.HttpUriRequest;
import com.tracelytics.ext.apache.http.config.Lookup;
import com.tracelytics.ext.apache.http.conn.routing.RouteInfo;
import com.tracelytics.ext.apache.http.cookie.Cookie;
import com.tracelytics.ext.apache.http.cookie.CookieOrigin;
import com.tracelytics.ext.apache.http.cookie.CookieSpec;
import com.tracelytics.ext.apache.http.cookie.CookieSpecProvider;
import com.tracelytics.ext.apache.http.protocol.HttpContext;
import com.tracelytics.ext.apache.http.util.Args;
import com.tracelytics.ext.apache.http.util.TextUtils;

/**
 * Request interceptor that matches cookies available in the current
 * {@link CookieStore} to the request being executed and generates
 * corresponding {@code Cookie} request headers.
 *
 * @since 4.0
 */
@Immutable
public class RequestAddCookies implements HttpRequestInterceptor {

    private final Logger log = Logger.getLogger(getClass().getName());

    public RequestAddCookies() {
        super();
    }

    @Override
    public void process(final HttpRequest request, final HttpContext context)
            throws HttpException, IOException {
        Args.notNull(request, "HTTP request");
        Args.notNull(context, "HTTP context");

        final String method = request.getRequestLine().getMethod();
        if (method.equalsIgnoreCase("CONNECT")) {
            return;
        }

        final HttpClientContext clientContext = HttpClientContext.adapt(context);

        // Obtain cookie store
        final CookieStore cookieStore = clientContext.getCookieStore();
        if (cookieStore == null) {
            this.log.log(Level.FINE, "Cookie store not specified in HTTP context");
            return;
        }

        // Obtain the registry of cookie specs
        final Lookup<CookieSpecProvider> registry = clientContext.getCookieSpecRegistry();
        if (registry == null) {
            this.log.log(Level.FINE, "CookieSpec registry not specified in HTTP context");
            return;
        }

        // Obtain the target host, possibly virtual (required)
        final HttpHost targetHost = clientContext.getTargetHost();
        if (targetHost == null) {
            this.log.log(Level.FINE, "Target host not set in the context");
            return;
        }

        // Obtain the route (required)
        final RouteInfo route = clientContext.getHttpRoute();
        if (route == null) {
            this.log.log(Level.FINE, "Connection route not set in the context");
            return;
        }

        final RequestConfig config = clientContext.getRequestConfig();
        String policy = config.getCookieSpec();
        if (policy == null) {
            policy = CookieSpecs.DEFAULT;
        }
        if (this.log.isLoggable(Level.FINE)) {
            this.log.log(Level.FINE, "CookieSpec selected: " + policy);
        }

        URI requestURI = null;
        if (request instanceof HttpUriRequest) {
            requestURI = ((HttpUriRequest) request).getURI();
        } else {
            try {
                requestURI = new URI(request.getRequestLine().getUri());
            } catch (final URISyntaxException ignore) {
            }
        }
        final String path = requestURI != null ? requestURI.getPath() : null;
        final String hostName = targetHost.getHostName();
        int port = targetHost.getPort();
        if (port < 0) {
            port = route.getTargetHost().getPort();
        }

        final CookieOrigin cookieOrigin = new CookieOrigin(
                hostName,
                port >= 0 ? port : 0,
                !TextUtils.isEmpty(path) ? path : "/",
                route.isSecure());

        // Get an instance of the selected cookie policy
        final CookieSpecProvider provider = registry.lookup(policy);
        if (provider == null) {
            if (this.log.isLoggable(Level.FINE)) {
                this.log.log(Level.FINE, "Unsupported cookie policy: " + policy);
            }

            return;
        }
        final CookieSpec cookieSpec = provider.create(clientContext);
        // Get all cookies available in the HTTP state
        final List<Cookie> cookies = cookieStore.getCookies();
        // Find cookies matching the given origin
        final List<Cookie> matchedCookies = new ArrayList<Cookie>();
        final Date now = new Date();
        boolean expired = false;
        for (final Cookie cookie : cookies) {
            if (!cookie.isExpired(now)) {
                if (cookieSpec.match(cookie, cookieOrigin)) {
                    if (this.log.isLoggable(Level.FINE)) {
                        this.log.log(Level.FINE, "Cookie " + cookie + " match " + cookieOrigin);
                    }
                    matchedCookies.add(cookie);
                }
            } else {
                if (this.log.isLoggable(Level.FINE)) {
                    this.log.log(Level.FINE, "Cookie " + cookie + " expired");
                }
                expired = true;
            }
        }
        // Per RFC 6265, 5.3
        // The user agent must evict all expired cookies if, at any time, an expired cookie
        // exists in the cookie store
        if (expired) {
            cookieStore.clearExpired(now);
        }
        // Generate Cookie request headers
        if (!matchedCookies.isEmpty()) {
            final List<Header> headers = cookieSpec.formatCookies(matchedCookies);
            for (final Header header : headers) {
                request.addHeader(header);
            }
        }

        final int ver = cookieSpec.getVersion();
        if (ver > 0) {
            final Header header = cookieSpec.getVersionHeader();
            if (header != null) {
                // Advertise cookie version support
                request.addHeader(header);
            }
        }

        // Stick the CookieSpec and CookieOrigin instances to the HTTP context
        // so they could be obtained by the response interceptor
        context.setAttribute(HttpClientContext.COOKIE_SPEC, cookieSpec);
        context.setAttribute(HttpClientContext.COOKIE_ORIGIN, cookieOrigin);
    }

}
