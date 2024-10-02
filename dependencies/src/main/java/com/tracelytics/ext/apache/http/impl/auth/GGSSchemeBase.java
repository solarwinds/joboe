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
package com.tracelytics.ext.apache.http.impl.auth;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSManager;
import org.ietf.jgss.GSSName;
import org.ietf.jgss.Oid;

import com.tracelytics.ext.apache.http.Header;
import com.tracelytics.ext.apache.http.HttpHost;
import com.tracelytics.ext.apache.http.HttpRequest;
import com.tracelytics.ext.apache.http.annotation.NotThreadSafe;
import com.tracelytics.ext.apache.http.auth.AUTH;
import com.tracelytics.ext.apache.http.auth.AuthenticationException;
import com.tracelytics.ext.apache.http.auth.Credentials;
import com.tracelytics.ext.apache.http.auth.InvalidCredentialsException;
import com.tracelytics.ext.apache.http.auth.KerberosCredentials;
import com.tracelytics.ext.apache.http.auth.MalformedChallengeException;
import com.tracelytics.ext.apache.http.client.protocol.HttpClientContext;
import com.tracelytics.ext.apache.http.conn.routing.HttpRoute;
import com.tracelytics.ext.apache.http.message.BufferedHeader;
import com.tracelytics.ext.apache.http.protocol.HttpContext;
import com.tracelytics.ext.apache.http.util.Args;
import com.tracelytics.ext.apache.http.util.CharArrayBuffer;
import com.tracelytics.ext.base64.Base64;

/**
 * @since 4.2
 */
@NotThreadSafe
public abstract class GGSSchemeBase extends AuthSchemeBase {

    enum State {
        UNINITIATED,
        CHALLENGE_RECEIVED,
        TOKEN_GENERATED,
        FAILED,
    }

    private final Logger log = Logger.getLogger(getClass().getName());

    private final boolean stripPort;
    private final boolean useCanonicalHostname;

    /** Authentication process state */
    private State state;

    /** base64 decoded challenge **/
    private byte[] token;

    GGSSchemeBase(final boolean stripPort, final boolean useCanonicalHostname) {
        super();
        this.stripPort = stripPort;
        this.useCanonicalHostname = useCanonicalHostname;
        this.state = State.UNINITIATED;
    }

    GGSSchemeBase(final boolean stripPort) {
        this(stripPort, true);
    }

    GGSSchemeBase() {
        this(true,true);
    }

    protected GSSManager getManager() {
        return GSSManager.getInstance();
    }

    protected byte[] generateGSSToken(
            final byte[] input, final Oid oid, final String authServer) throws GSSException {
        return generateGSSToken(input, oid, authServer, null);
    }

    /**
     * @since 4.4
     */
    protected byte[] generateGSSToken(
            final byte[] input, final Oid oid, final String authServer,
            final Credentials credentials) throws GSSException {
        byte[] inputBuff = input;
        if (inputBuff == null) {
            inputBuff = new byte[0];
        }
        final GSSManager manager = getManager();
        final GSSName serverName = manager.createName("HTTP@" + authServer, GSSName.NT_HOSTBASED_SERVICE);

        final GSSCredential gssCredential;
        if (credentials instanceof KerberosCredentials) {
            gssCredential = ((KerberosCredentials) credentials).getGSSCredential();
        } else {
            gssCredential = null;
        }

        final GSSContext gssContext = manager.createContext(
                serverName.canonicalize(oid), oid, gssCredential, GSSContext.DEFAULT_LIFETIME);
        gssContext.requestMutualAuth(true);
        gssContext.requestCredDeleg(true);
        return gssContext.initSecContext(inputBuff, 0, inputBuff.length);
    }

    /**
     * @deprecated (4.4) Use {@link #generateToken(byte[], String, com.tracelytics.ext.apache.http.auth.Credentials)}.
     */
    @Deprecated
    protected byte[] generateToken(final byte[] input, final String authServer) throws GSSException {
        return null;
    }

    /**
     * @since 4.4
     */
    //TODO: make this method abstract
    @SuppressWarnings("deprecation")
    protected byte[] generateToken(
            final byte[] input, final String authServer, final Credentials credentials) throws GSSException {
        return generateToken(input, authServer);
    }

    @Override
    public boolean isComplete() {
        return this.state == State.TOKEN_GENERATED || this.state == State.FAILED;
    }

    /**
     * @deprecated (4.2) Use {@link com.tracelytics.ext.apache.http.auth.ContextAwareAuthScheme#authenticate(
     *   Credentials, HttpRequest, com.tracelytics.ext.apache.http.protocol.HttpContext)}
     */
    @Override
    @Deprecated
    public Header authenticate(
            final Credentials credentials,
            final HttpRequest request) throws AuthenticationException {
        return authenticate(credentials, request, null);
    }

    @Override
    public Header authenticate(
            final Credentials credentials,
            final HttpRequest request,
            final HttpContext context) throws AuthenticationException {
        Args.notNull(request, "HTTP request");
        switch (state) {
        case UNINITIATED:
            throw new AuthenticationException(getSchemeName() + " authentication has not been initiated");
        case FAILED:
            throw new AuthenticationException(getSchemeName() + " authentication has failed");
        case CHALLENGE_RECEIVED:
            try {
                final HttpRoute route = (HttpRoute) context.getAttribute(HttpClientContext.HTTP_ROUTE);
                if (route == null) {
                    throw new AuthenticationException("Connection route is not available");
                }
                HttpHost host;
                if (isProxy()) {
                    host = route.getProxyHost();
                    if (host == null) {
                        host = route.getTargetHost();
                    }
                } else {
                    host = route.getTargetHost();
                }
                final String authServer;
                String hostname = host.getHostName();

                if (this.useCanonicalHostname){
                    try {
                         //TODO: uncomment this statement and delete the resolveCanonicalHostname,
                         //TODO: as soon canonical hostname resolving is implemented in the SystemDefaultDnsResolver
                         //final DnsResolver dnsResolver = SystemDefaultDnsResolver.INSTANCE;
                         //hostname = dnsResolver.resolveCanonicalHostname(host.getHostName());
                         hostname = resolveCanonicalHostname(hostname);
                    } catch (UnknownHostException ignore){
                    }
                }
                if (this.stripPort) { // || host.getPort()==80 || host.getPort()==443) {
                    authServer = hostname;
                } else {
                    authServer = hostname + ":" + host.getPort();
                }

                if (log.isLoggable(Level.FINE)) {
                    log.log(Level.FINE, "init " + authServer);
                }
                token = generateToken(token, authServer, credentials);
                state = State.TOKEN_GENERATED;
            } catch (final GSSException gsse) {
                state = State.FAILED;
                if (gsse.getMajor() == GSSException.DEFECTIVE_CREDENTIAL
                        || gsse.getMajor() == GSSException.CREDENTIALS_EXPIRED) {
                    throw new InvalidCredentialsException(gsse.getMessage(), gsse);
                }
                if (gsse.getMajor() == GSSException.NO_CRED ) {
                    throw new InvalidCredentialsException(gsse.getMessage(), gsse);
                }
                if (gsse.getMajor() == GSSException.DEFECTIVE_TOKEN
                        || gsse.getMajor() == GSSException.DUPLICATE_TOKEN
                        || gsse.getMajor() == GSSException.OLD_TOKEN) {
                    throw new AuthenticationException(gsse.getMessage(), gsse);
                }
                // other error
                throw new AuthenticationException(gsse.getMessage());
            }
        case TOKEN_GENERATED:
            final String tokenstr = Base64.encodeBytes(token);
            if (log.isLoggable(Level.FINE)) {
                log.log(Level.FINE, "Sending response '" + tokenstr + "' back to the auth server");
            }
            final CharArrayBuffer buffer = new CharArrayBuffer(32);
            if (isProxy()) {
                buffer.append(AUTH.PROXY_AUTH_RESP);
            } else {
                buffer.append(AUTH.WWW_AUTH_RESP);
            }
            buffer.append(": Negotiate ");
            buffer.append(tokenstr);
            return new BufferedHeader(buffer);
        default:
            throw new IllegalStateException("Illegal state: " + state);
        }
    }

    @Override
    protected void parseChallenge(
            final CharArrayBuffer buffer,
            final int beginIndex, final int endIndex) throws MalformedChallengeException {
        final String challenge = buffer.substringTrimmed(beginIndex, endIndex);
        if (log.isLoggable(Level.FINE)) {
            log.log(Level.FINE, "Received challenge '" + challenge + "' from the auth server");
        }
        if (state == State.UNINITIATED) {
            token = Base64.encodeBytesToBytes(challenge.getBytes());
            state = State.CHALLENGE_RECEIVED;
        } else {
            log.log(Level.FINE, "Authentication already attempted");
            state = State.FAILED;
        }
    }

    private String resolveCanonicalHostname(final String host) throws UnknownHostException {
        final InetAddress in = InetAddress.getByName(host);
        final String canonicalServer = in.getCanonicalHostName();
        if (in.getHostAddress().contentEquals(canonicalServer)) {
            return host;
        }
        return canonicalServer;
    }

}
