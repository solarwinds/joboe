package com.appoptics.test;

import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.glassfish.grizzly.http.server.TimeoutHandler;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.concurrent.TimeUnit;

public class NonBlockingHandler extends HttpHandler {
    private static final String OUTPUT_STRING = "Hello world!";
    private static final int REPEAT_COUNT = 100;
    private static final int PAUSE_IN_MILLISECOND = 100;

    @Override
    public void service(final Request request,
                        final Response response) throws Exception {

        final char[] buf = new char[128];
        final Reader in = request.getReader(false); // return the non-blocking InputStream
        final Writer out = response.getWriter();

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

        new Thread() {
            @Override
            public void run() {
                try {
                    slowWriteData(out);
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    response.resume();
                }

            }
        }.start();


    }

    private void slowWriteData(Writer out) throws IOException, InterruptedException {
        for (int i = 0; i < REPEAT_COUNT; i ++) {
            out.write(OUTPUT_STRING);
            TimeUnit.MILLISECONDS.sleep(PAUSE_IN_MILLISECOND);
        }
    }
}
