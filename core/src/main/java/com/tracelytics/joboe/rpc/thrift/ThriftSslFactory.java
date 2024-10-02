/**
 * Reports events through UDP
 */
package com.tracelytics.joboe.rpc.thrift;

import com.appoptics.ext.org.apache.thrift.transport.TSocket;
import com.appoptics.ext.org.apache.thrift.transport.TTransportException;
import com.tracelytics.logging.Logger;
import com.tracelytics.logging.LoggerFactory;
import com.tracelytics.util.SslUtils;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.util.HashSet;
import java.util.Set;

/**
 * Creates {@link TSocket} with SSL 
 * @author pluk
 *
 */
class ThriftSslFactory {
    private static final Logger logger = LoggerFactory.getLogger();

    private final SSLContext context;
    

    ThriftSslFactory(URL serverCertLocation, String explicitHostCheck) throws IOException, GeneralSecurityException {
        context = SslUtils.getSslContext(serverCertLocation, explicitHostCheck);
    }


    TSocket getSslTSocket(String host, int port, int timeout) throws ThriftClientConnectException {
        try {
            // Create a socket without connecting
            SSLSocket socket = (SSLSocket) context.getSocketFactory().createSocket();
            
            // In order to avoid SSL version problem, see http://www.oracle.com/technetwork/java/javase/documentation/cve-2014-3566-2342133.html
            Set<String> set = new HashSet<String>();
            for (String s : socket.getEnabledProtocols()) { 
                 if (s.equals("SSLv3") || s.equals("SSLv2Hello")) {
                    continue;
                }
                set.add(s);
            }

            socket.setEnabledProtocols(set.toArray(new String[0]));

            // Connect, with an explicit timeout value
            socket.connect(new InetSocketAddress(host, port), timeout);
            
            TSocket tSocket = new TSocket(socket);
            tSocket.setTimeout(timeout);
            
            return tSocket;
        } catch (TTransportException e) {
            throw new ThriftClientConnectException(e);
        } catch (UnknownHostException e) {
            throw new ThriftClientConnectException(e);
        } catch (IOException e) {
            throw new ThriftClientConnectException(e);
        }
    }


}
