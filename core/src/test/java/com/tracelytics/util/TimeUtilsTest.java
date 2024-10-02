package com.tracelytics.util;

import junit.framework.TestCase;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class TimeUtilsTest extends TestCase {
    private static final int RUN_COUNT = 10000;
    private static final int WARM_UP_COUNT = 1000;
    
    public void testTimeBase() {
        List<Long> diffsInMicroSec = new ArrayList<Long>();
        
        for (int i = 0 ; i < RUN_COUNT; i ++) {
            long timeUtilsMirco = TimeUtils.getTimestampMicroSeconds();
            long systemMilli = System.currentTimeMillis();
        
            if (i > WARM_UP_COUNT) {
                diffsInMicroSec.add(Math.abs(systemMilli * 1000 - timeUtilsMirco));
            }
        }
        
        TimeUtils.Statistics<Long> statistics = new TimeUtils.Statistics<Long>(diffsInMicroSec);
        
        long median = statistics.getMedian().longValue();
        assertTrue("Found median " + median, median < 100000); //a rather generous median, this test is just to make sure value is not totally incorrect as unstable system (especially windows) can sometimes give bad shift
    }
    
    

    public void testTimeDifference() throws InterruptedException {
        List<Long> durationDiscrepanciesInMicroSec;
        TimeUtils.Statistics<Long> statistics;
        
        
        durationDiscrepanciesInMicroSec = new ArrayList<Long>();
        for (int i = 0 ; i < RUN_COUNT; i ++) {
            long startMicro = TimeUtils.getTimestampMicroSeconds();
            Thread.sleep(1);
            long endMicro = TimeUtils.getTimestampMicroSeconds();

            assertTrue("end micro is smaller than startMicro: " + endMicro + " vs " + startMicro, endMicro > startMicro); //this is the most important! Time should not go backwards
            
            if (i > WARM_UP_COUNT) {
                durationDiscrepanciesInMicroSec.add(Math.abs((endMicro - startMicro) - 1000));
            }
            
        }
        
        statistics = new TimeUtils.Statistics<Long>(durationDiscrepanciesInMicroSec);
        long differenceMedian = statistics.getMedian().longValue();
        assertTrue("Found median " + differenceMedian, differenceMedian < 1000); //discrepancy of p95 should be less than 1 millisec

        //below case commented out as the sleep time is too low and other factors affect the result too significantly 
//        durationDiscrepanciesInMicroSec = new double[RUN_COUNT];
//        for (int i = 0 ; i < RUN_COUNT; i ++) {
//            long startMicro = TimeUtils.getTimestampMicroSeconds();
//            TimeUnit.MICROSECONDS.sleep(100);
//            long endMicro = TimeUtils.getTimestampMicroSeconds();
//            
//            assertTrue(endMicro > startMicro);
//            
//            durationDiscrepanciesInMicroSec[i] = Math.abs((endMicro - startMicro) - 100);
//            System.out.println(durationDiscrepanciesInMicroSec[i]);
//            
//        }
//        
//        statistics = new Statistics(durationDiscrepanciesInMicroSec);
//        p95 = statistics.getPercentile(0.95);
//        
//        assertTrue("Found p95 " + p95, p95 < 100); //discrepancy of p95 should be less than 0.1 millisec
            
    }
    
    public void testTimeAdjustWorkerInit() throws Exception {
        Method startAdjustBaseWorkerMethod = TimeUtils.class.getDeclaredMethod("startAdjustBaseWorker", Integer.class);
        startAdjustBaseWorkerMethod.setAccessible(true);
        
        assertFalse((Boolean) startAdjustBaseWorkerMethod.invoke(null, 0));
        assertTrue((Boolean) startAdjustBaseWorkerMethod.invoke(null, 100));
    }
}


