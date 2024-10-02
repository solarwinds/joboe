/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.tracelytics.ext.apache.thrift.transport;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.thrift.TByteArrayOutputStream;
import com.tracelytics.ext.jcraft.jzlib.Deflater;
import com.tracelytics.ext.jcraft.jzlib.DeflaterOutputStream;
import com.tracelytics.ext.jcraft.jzlib.Inflater;
import com.tracelytics.ext.jcraft.jzlib.JZlib;
import com.tracelytics.ext.jcraft.jzlib.ZStreamException;
import org.apache.thrift.transport.*;

/**
 * TZlibTransport uses zlib's compressed format on the "far" side.
 * 
 * Take note that this is extracted from https://issues.apache.org/jira/secure/attachment/12633257/thrift_java_tzlibtransport_added_jzlib_v1.1.3.patch
 * which was a proposed solution of TZlibTransport that uses a 3rd party library (instead of JDK 7's Deflator)
 * 
 * This alternative TZlibTransport is added as for backward compatibility for JDK 6-
 */
public class TZlibTransport2 extends TTransport {

    /**
     * Underlying transport
     */
    private TTransport transport_ = null;
    /**
     * Buffer for output
     */
    private final TByteArrayOutputStream writeBuffer_ = new TByteArrayOutputStream(1024);
    /**
     * Buffer for input
     */
    private TMemoryInputTransport readBuffer_ = new TMemoryInputTransport(new byte[0]);
    public static int DEFAULT_URBUF_SIZE = 128;
    public static int DEFAULT_CRBUF_SIZE = 1024;
    public static int DEFAULT_UWBUF_SIZE = 128;
    public static int DEFAULT_CWBUF_SIZE = 1024;
    private Inflater zlibReadInflater = null;
    private int urbufSize = DEFAULT_URBUF_SIZE;
    private byte[] urbuf = new byte[urbufSize];
    private int crbufSize = DEFAULT_CRBUF_SIZE;
    private byte[] crbuf = new byte[crbufSize];
    // private ZStream zw=new ZStream();
    // private int uwbufSize=DEFAULT_UWBUF_SIZE;
    // private byte[] uwbuf= new byte[uwbufSize];
    // private int cwbufSize=DEFAULT_CWBUF_SIZE;
    // private byte[] cwbuf= new byte[cwbufSize];
    private DeflaterOutputStream zlibWriteDeflaterOS = null;
    private ByteArrayOutputStream cwbufOS = null;

    public static class Factory extends TTransportFactory {

        public Factory() {
        }

        @Override
        public TTransport getTransport(TTransport base) {
            return new TZlibTransport(base);
        }
    }

    public TZlibTransport2(TTransport transport) throws IOException {
        transport_ = transport;

        initZlib();
    }

    public void open() throws TTransportException {
        transport_.open();
    }

    public boolean isOpen() {
        return transport_.isOpen();
    }

    public void close() {
        readBuffer_.reset(new byte[0]);
        writeBuffer_.reset();
        transport_.close();
    }

    public int read(byte[] buf, int off, int len) throws TTransportException {
        if (readBuffer_ != null) {
            int got = readBuffer_.read(buf, off, len);
            if (got > 0) {
                return got;
            }
        }

        while (true) {
            if (readComp() > 0) {
                break;
            }
        }

        return readBuffer_.read(buf, off, len);
    }

    private int readComp() throws TTransportException {

        if (!(zlibReadInflater.avail_in > 0 && zlibReadInflater.avail_out == 0)) {

            // System.out.println("TzlibTransport.readComp: Trying to read...");
            int bytesRead = transport_.read(crbuf, 0, crbufSize);
            // System.out.println("TzlibTransport.readComp: Reading successful (read: " + bytesRead + " of requested: " + crbufSize + " bytes)");

            zlibReadInflater.next_in = crbuf;
            zlibReadInflater.next_in_index = 0;
            zlibReadInflater.avail_in = bytesRead;
        }

        zlibReadInflater.next_out = urbuf;
        zlibReadInflater.next_out_index = 0;
        zlibReadInflater.avail_out = urbufSize;

        int err = zlibReadInflater.inflate(JZlib.Z_NO_FLUSH);

        if (err != JZlib.Z_OK) {
            throw new TTransportException(new ZStreamException(zlibReadInflater.msg));
        }

        byte[] old = new byte[readBuffer_.getBytesRemainingInBuffer()];
        int oldBytesRead = readBuffer_.read(old, 0, readBuffer_.getBytesRemainingInBuffer());

        byte[] all = new byte[old.length + zlibReadInflater.next_out_index];
        System.arraycopy(old, 0, all, 0, old.length);
        System.arraycopy(urbuf, 0, all, old.length, zlibReadInflater.next_out_index);

        readBuffer_.reset(all);
        // System.out.println("TzlibTransport.readComp: returning: " + all.length + " bytes");
        // System.out.println("TzlibTransport.readComp: read compression ratio: " + zlibReadInflater.total_in + "/" + zlibReadInflater.total_out + " bytes");
        return all.length;

    }

    @Override
    public byte[] getBuffer() {
        return readBuffer_.getBuffer();
    }

    @Override
    public int getBufferPosition() {
        return readBuffer_.getBufferPosition();
    }

    @Override
    public int getBytesRemainingInBuffer() {
        return readBuffer_.getBytesRemainingInBuffer();
    }

    @Override
    public void consumeBuffer(int len) {
        readBuffer_.consumeBuffer(len);
    }

    public void write(byte[] buf, int off, int len) throws TTransportException {
        writeBuffer_.write(buf, off, len);
    }

    @Override
    public void flush() throws TTransportException {

        try {

            zlibWriteDeflaterOS.setSyncFlush(true);
            zlibWriteDeflaterOS.write(writeBuffer_.get(), 0, writeBuffer_.len());

            zlibWriteDeflaterOS.flush();

            transport_.write(cwbufOS.toByteArray(), 0, cwbufOS.size());
            cwbufOS.reset();

            transport_.flush();

            writeBuffer_.reset();

        } catch (IOException ex) {
            throw new TTransportException(ex);
        }

    }

    private void initZlib() throws IOException {
        zlibReadInflater = new Inflater();
        zlibReadInflater.inflateInit(false);
        zlibReadInflater.next_in = crbuf;
        zlibReadInflater.next_in_index = 0;
        zlibReadInflater.avail_in = 0;
        zlibReadInflater.next_out = urbuf;
        zlibReadInflater.next_out_index = 0;
        zlibReadInflater.avail_out = urbufSize;

        cwbufOS = new ByteArrayOutputStream();
        Deflater deflater = new Deflater(JZlib.Z_DEFAULT_COMPRESSION, false);
        zlibWriteDeflaterOS = new DeflaterOutputStream(cwbufOS, deflater);
    }
}