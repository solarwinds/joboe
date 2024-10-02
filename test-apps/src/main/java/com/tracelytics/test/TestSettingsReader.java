/**
 * Reads and dumps Sample Rate
 */

package com.tracelytics.test;

import java.util.concurrent.atomic.AtomicInteger;

import com.tracelytics.joboe.LayerUtil;
import com.tracelytics.joboe.SampleRateConfig;

public class TestSettingsReader implements Runnable {

    private static AtomicInteger threadCount = new AtomicInteger();
    static AtomicInteger reqCount = new AtomicInteger();
    private String layerName;
    private int numThreads;
    private int delay;

    public TestSettingsReader(String layerName, int numThreads, int delay) {
        this.layerName = layerName;
        this.numThreads = numThreads;
        this.delay = delay;
    }

    public void start() {
        new Thread(new SettingsStats()).start();

        for(int i=0; i<numThreads; i++) {
            Thread thr = new Thread(this);
            thr.start();
        }
    } 

    /* Starts tracing, sending start/end events, calls into another layer */
    public void run() {
        boolean running = true;
        int threadNum = threadCount.incrementAndGet();
        int lastValue = -1;

        while(running) {
            SampleRateConfig cfg = null;
            cfg = LayerUtil.getLocalSampleRate(layerName, null);
            reqCount.incrementAndGet();
            int value = cfg.getSampleRate();
            if (value != lastValue) {
                log(threadNum, "value=" + value + " source=" + cfg.getSampleRateSource());
                lastValue = value;
            }

            if (delay > 0) {
                try {
                    Thread.sleep(delay);
                } catch(Exception ex) {
                    // Ignore
                }
            }
        }

         
    }

    public void log(int threadNum, String msg) {
        System.out.println("Thread " + threadNum + ": " + msg);
    }

    public static void main(String args[]) throws Exception {
        if (args.length < 3) {
            System.out.println("Usage: TestSettingsReader layerName numberOfThreads delay");
            System.exit(1);
        }
    
        TestSettingsReader test = new TestSettingsReader(args[0], Integer.parseInt(args[1]), Integer.parseInt(args[2]));
        test.start();
    }
}

class SettingsStats implements Runnable {

    public SettingsStats() {
    }

    public void run() {
        int last = 0, current = 0;
        while(true) {
            try {
                Thread.sleep(1000);
                current = TestSettingsReader.reqCount.get();
                System.out.println("reqs/sec: " + (current-last));
                last = current;
            } catch(Exception ex) {
                // ignore 
            }
        }
    }
}
