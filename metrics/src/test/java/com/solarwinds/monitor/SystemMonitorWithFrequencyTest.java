package com.solarwinds.monitor;

import com.solarwinds.joboe.config.InvalidConfigException;
import com.solarwinds.monitor.SystemMonitorWithFrequency.TimeUnit;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class SystemMonitorWithFrequencyTest {

    @Test
    public void testGetSleep() throws Exception {
        Method getSleepTimeMethod = SystemMonitorWithFrequency.class.getDeclaredMethod("getSleepTime", long.class);
        getSleepTimeMethod.setAccessible(true);
        
        SimpleDateFormat dataFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.CANADA); 
        long testTime;
        
        //set at the midnight
        testTime = dataFormat.parse("2000-01-01 00:00:00.000").getTime();
        assertEquals((long)6 * 60 * 1000, getSleepTimeMethod.invoke(new TestingSystemMonitorWithFequency(TimeUnit.PER_HOUR, 10), testTime)); //report at next mark
        assertEquals((long)30 * 1000, getSleepTimeMethod.invoke(new TestingSystemMonitorWithFequency(TimeUnit.PER_MINUTE, 2), testTime)); //report at next mark
        assertEquals((long)100, getSleepTimeMethod.invoke(new TestingSystemMonitorWithFequency(TimeUnit.PER_SECOND, 10), testTime)); //report at next mark
        
        //set at the 01:00:00
        testTime = dataFormat.parse("2000-01-01 01:00:00.000").getTime();
        assertEquals((long)6 * 60 * 1000, getSleepTimeMethod.invoke(new TestingSystemMonitorWithFequency(TimeUnit.PER_HOUR, 10), testTime)); //report at next mark 
        assertEquals((long)30 * 1000, getSleepTimeMethod.invoke(new TestingSystemMonitorWithFequency(TimeUnit.PER_MINUTE, 2), testTime)); //report at next mark
        assertEquals((long)100, getSleepTimeMethod.invoke(new TestingSystemMonitorWithFequency(TimeUnit.PER_SECOND, 10), testTime)); //report at next mark
        
        //set at the 00:01:00
        testTime = dataFormat.parse("2000-01-01 00:01:00.000").getTime();
        assertEquals((long)5 * 60 * 1000, getSleepTimeMethod.invoke(new TestingSystemMonitorWithFequency(TimeUnit.PER_HOUR, 10), testTime)); //should report at 00:06:00, sleep for 5 mins
        assertEquals((long)30 * 1000, getSleepTimeMethod.invoke(new TestingSystemMonitorWithFequency(TimeUnit.PER_MINUTE, 2), testTime)); //report at next mark 
        assertEquals((long)100, getSleepTimeMethod.invoke(new TestingSystemMonitorWithFequency(TimeUnit.PER_SECOND, 10), testTime)); //report at next mark 
        
        //set at the 00:00:01
        testTime = dataFormat.parse("2000-01-01 00:00:01.000").getTime();
        assertEquals((long)(6 * 60 -1) * 1000, getSleepTimeMethod.invoke(new TestingSystemMonitorWithFequency(TimeUnit.PER_HOUR, 10), testTime)); //should report at 00:06:00, sleep for 5 mins 59 secs 
        assertEquals((long)29 * 1000, getSleepTimeMethod.invoke(new TestingSystemMonitorWithFequency(TimeUnit.PER_MINUTE, 2), testTime)); //should report at 00:00:30, sleep for 29 secs
        assertEquals((long)100, getSleepTimeMethod.invoke(new TestingSystemMonitorWithFequency(TimeUnit.PER_SECOND, 10), testTime)); //report at next mark
        
        //set at the 00:00:00.001
        testTime = dataFormat.parse("2000-01-01 00:00:00.001").getTime();
        assertEquals((long)6 * 60 * 1000 - 1, getSleepTimeMethod.invoke(new TestingSystemMonitorWithFequency(TimeUnit.PER_HOUR, 10), testTime)); //should report at 00:06:00, sleep for 5 mins 59.999 secs 
        assertEquals((long)30 * 1000 - 1, getSleepTimeMethod.invoke(new TestingSystemMonitorWithFequency(TimeUnit.PER_MINUTE, 2), testTime)); //should report at 00:00:30, sleep for 29.999 secs
        assertEquals((long)99, getSleepTimeMethod.invoke(new TestingSystemMonitorWithFequency(TimeUnit.PER_SECOND, 10), testTime)); //should report at 00:00:00.100, sleep for 99 ms
            
        
        //set at the 23:59:59.999
        testTime = dataFormat.parse("2000-01-01 23:59:59.999").getTime();
        assertEquals((long)1, getSleepTimeMethod.invoke(new TestingSystemMonitorWithFequency(TimeUnit.PER_HOUR, 10), testTime)); //sleep for 1 ms
        assertEquals((long)1, getSleepTimeMethod.invoke(new TestingSystemMonitorWithFequency(TimeUnit.PER_MINUTE, 2), testTime)); //sleep for 1 ms
        assertEquals((long)1, getSleepTimeMethod.invoke(new TestingSystemMonitorWithFequency(TimeUnit.PER_SECOND, 10), testTime)); //sleep for 1 ms
    }

    @Test
    public void testSetInterval() throws Exception {
        Method getSleepTimeMethod = SystemMonitorWithFrequency.class.getDeclaredMethod("getSleepTime", long.class);
        getSleepTimeMethod.setAccessible(true);
        
        SimpleDateFormat dataFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.CANADA); 
        long testTime = dataFormat.parse("2000-01-01 00:00:01.000").getTime();
        TestingSystemMonitorWithFequency monitor = new TestingSystemMonitorWithFequency(TimeUnit.PER_MINUTE, 2);
        monitor.setInterval(10 * 1000); //10 secs
        assertEquals((long) (10 - 1) * 1000, getSleepTimeMethod.invoke(monitor, testTime)); //sleep for 9 sec
        monitor.setInterval(60 * 1000); // 1 min
        assertEquals((long) (60 - 1) * 1000, getSleepTimeMethod.invoke(monitor, testTime)); //sleep for 59 sec
        monitor.setInterval(10 * 60 * 1000); // 10 min
        assertEquals((long) (10 * 60  - 1) * 1000, getSleepTimeMethod.invoke(monitor, testTime)); //sleep for 599 sec
        try {
            monitor.setInterval(7 * 1000); //7 secs, not valid
            fail();
        } catch (InvalidConfigException e) {
            //expected
        }
    }
    
    private class TestingSystemMonitorWithFequency extends SystemMonitorWithFrequency<Object, Object> {
        public TestingSystemMonitorWithFequency(TimeUnit timeUnit, int frequency) throws InvalidConfigException {
            super(timeUnit, frequency, null, null);
        }

        @Override
        protected String getMonitorName() {
            // TODO Auto-generated method stub
            return null;
        }
    }
    
    
    
   
}
