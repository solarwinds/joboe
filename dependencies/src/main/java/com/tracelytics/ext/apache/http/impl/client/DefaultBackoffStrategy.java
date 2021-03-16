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
package com.tracelytics.ext.apache.http.impl.client;

import java.net.ConnectException;
import java.net.SocketTimeoutException;

import com.tracelytics.ext.apache.http.HttpResponse;
import com.tracelytics.ext.apache.http.HttpStatus;

import com.tracelytics.ext.apache.http.client.ConnectionBackoffStrategy;

/**
 * This {@link ConnectionBackoffStrategy} backs off either for a raw
 * network socket or connection timeout or if the server explicitly
 * sends a 503 (Service Unavailable) response.
 *
 * @since 4.2
 */
public class DefaultBackoffStrategy implements ConnectionBackoffStrategy {

    @Override
    public boolean shouldBackoff(final Throwable t) {
        return (t instanceof SocketTimeoutException
                || t instanceof ConnectException);
    }

    @Override
    public boolean shouldBackoff(final HttpResponse resp) {
        return (resp.getStatusLine().getStatusCode() == HttpStatus.SC_SERVICE_UNAVAILABLE);
    }

}
