package com.appoptics.test;

import org.glassfish.grizzly.CompletionHandler;
import org.glassfish.grizzly.ReadHandler;
import org.glassfish.grizzly.http.io.NIOReader;
import org.glassfish.grizzly.http.io.NIOWriter;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.glassfish.grizzly.http.server.TimeoutHandler;


import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class NonBlockingHandler extends HttpHandler {
    private static final String OUTPUT_STRING = "Hello world!";
    private static final int REPEAT_COUNT = 100;
    private static final int PAUSE_IN_MILLISECOND = 100;

    @Override
    public void service(final Request request,
                        final Response response) throws Exception {

        final char[] buf = new char[128];
        final NIOReader in = request.getNIOReader(); // return the non-blocking InputStream
        final NIOWriter out = response.getNIOWriter();

        Integer timeout = null;
        if (request.getParameter("timeout") != null) {
            timeout = Integer.parseInt(request.getParameter("timeout"));
        }

        if (timeout != null) {
            response.suspend(timeout, TimeUnit.MILLISECONDS, null, new TimeoutHandler() {
                @Override
                public boolean onTimeout(Response response) {
                    try {
                        out.close();
                    } catch (IOException ignored) {
                    }
                    return true;
                }
            });
        } else {
            response.suspend();
        }

        // If we don't have more data to read - onAllDataRead() will be called
        in.notifyAvailable(new ReadHandler() {

            @Override
            public void onDataAvailable() throws Exception {
                readData(in, buf);
                in.notifyAvailable(this);
            }

            @Override
            public void onError(Throwable t) {
                System.out.println("[onError]" + t);
                response.resume();
            }

            @Override
            public void onAllDataRead() throws Exception {
                System.out.printf("[onAllDataRead] length: %d\n", in.readyData());
                try {
                    readData(in, buf);
                    slowWriteData(out);
                } finally {
                    try {
                        in.close();
                    } catch (IOException ignored) {
                    }

                    try {
                        out.close();
                    } catch (IOException ignored) {
                    }

                    response.resume();
                }
            }
        });

    }

    private void slowWriteData(NIOWriter out) throws IOException, InterruptedException {
        for (int i = 0; i < REPEAT_COUNT; i ++) {
            out.write(OUTPUT_STRING);
            TimeUnit.MILLISECONDS.sleep(PAUSE_IN_MILLISECOND);
        }
    }

    private void readData(NIOReader in, char[] buf) throws IOException {
        while(in.isReady()) { //just read whatever
            int len = in.read(buf);
        }
    }


    private void echoAvailableData(NIOReader in, NIOWriter out, char[] buf)
            throws IOException {

        while(in.isReady()) {
            int len = in.read(buf);
        }
    }
}
