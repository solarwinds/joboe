package com.solarwinds.joboe.core.profiler;

import com.solarwinds.joboe.core.TestReporter;
import com.solarwinds.joboe.core.TestReporter.DeserializedEvent;
import com.solarwinds.joboe.core.profiler.Profiler.Profile;
import com.solarwinds.joboe.core.util.TestUtils;
import com.solarwinds.joboe.sampling.Metadata;
import com.solarwinds.joboe.sampling.SamplingConfiguration;
import org.junit.jupiter.api.BeforeEach;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ProfileTest {

    @BeforeEach
    void setup(){
        Metadata.setup(SamplingConfiguration.builder().build());
    }

    private final ProfilerSetting profilerSetting = new ProfilerSetting(true, Collections.EMPTY_SET, ProfilerSetting.DEFAULT_INTERVAL, ProfilerSetting.DEFAULT_CIRCUIT_BREAKER_DURATION_THRESHOLD, ProfilerSetting.DEFAULT_CIRCUIT_BREAKER_COUNT_THRESHOLD);
    private final TestReporter profilingReporter = TestUtils.initProfilingReporter(profilerSetting);

    private void methodA(Profile profile) {
        Thread thread = Thread.currentThread();
        StackTraceElement[] stackTrace = thread.getStackTrace();
        //printStack(stackTrace);
        
        profile.record(thread, stackTrace, 2);
        List<DeserializedEvent> sentEvents = profilingReporter.getSentEvents();
        assertEquals(1, profilingReporter.getSentEvents().size());
        Map<String, Object> snapshotEvent = sentEvents.get(0).getSentEntries();
        
        assertEquals(2, snapshotEvent.get("FramesExited")); //exited the `getStackTrace` from `testRecord` and the line in `testRecord` that calls `getStackTrace` 
        assertEquals(stackTrace.length, snapshotEvent.get("FramesCount"));
        assertEquals(thread.getId(), snapshotEvent.get("TID"));
        assertEquals(Collections.singletonMap("0", 1L), snapshotEvent.get("SnapshotsOmitted")); //omitted 1 snapshot in previous call
        
        assertNewFrames(Arrays.copyOfRange(stackTrace, 0 , 3), (Map<String, Map<String, Object>>) snapshotEvent.get("NewFrames")); //the top 3 new frames : getStackTrace, methodA and the line in `testRecord` that calls `methodA`
        
        profilingReporter.reset();
    }
    
    private void methodB(Profile profile) {
        Thread thread = Thread.currentThread();
        StackTraceElement[] stackTrace = thread.getStackTrace();
        profile.record(thread, stackTrace, 4);//nothing is sent as the truncated stack trace is identical to previous truncated stack trace
        
        assert(profilingReporter.getSentEvents().isEmpty()); 
    }
    
    private void recursion(Profile profile, int remainingIteration) {
        if (remainingIteration == 0) {
            Thread thread = Thread.currentThread();
            StackTraceElement[] stackTrace = thread.getStackTrace();
            //printStack(stackTrace);
            
            profile.record(thread, stackTrace, 3);
            List<DeserializedEvent> sentEvents = profilingReporter.getSentEvents();
            assertEquals(1, profilingReporter.getSentEvents().size());
            Map<String, Object> snapshotEvent = sentEvents.get(0).getSentEntries();
            
            assertEquals(3, snapshotEvent.get("FramesExited")); //exited the `getStackTrace` from `methodA`, `methodA`, and the line in `testRecord` that calls `methodA` 
            assertEquals(stackTrace.length, snapshotEvent.get("FramesCount"));
            assertEquals(thread.getId(), snapshotEvent.get("TID"));
            
            int lastRecursionFrameindex = 0;
            int walker = stackTrace.length - 1;
            while (walker >= 0) {
                StackTraceElement frame = stackTrace[walker];
                if ("recursion".equals(frame.getMethodName())) {
                    lastRecursionFrameindex = walker;
                }
                walker --;
            }
            
            StackTraceElement[] truncatedStackTrace = Arrays.copyOfRange(stackTrace, stackTrace.length - Profiler.MAX_REPORTED_FRAME_DEPTH, stackTrace.length);
            StackTraceElement[] newFrames = Arrays.copyOfRange(truncatedStackTrace, 0, lastRecursionFrameindex + 2); // +1 on the first call to Recursion then +1 on the line in `testRecord` that calls recursion
            assertNewFrames(newFrames, (Map<String, Map<String, Object>>) snapshotEvent.get("NewFrames")); //the top 3 new frames : getStackTrace, methodA and the line in `testRecord` that calls `methodA`
            
            profilingReporter.reset();
            
            methodB(profile);
        } else {
            recursion(profile, -- remainingIteration);
        }
    }
    
    private void printStack(StackTraceElement[] stackTrace) {
        System.out.println("==============");
        for (StackTraceElement frame : stackTrace) {
            System.out.println(frame);
        }
        
    }

    private void assertNewFrames(StackTraceElement[] expectedNewFrames, Map<String, Map<String, Object>> actualNewFrames) {
        for (int i = 0; i < expectedNewFrames.length; i ++) {
            StackTraceElement expectedFrame = expectedNewFrames[i];
            Map<String, Object> actualFrame = actualNewFrames.get(String.valueOf(i));
            assertEquals(expectedFrame.getClassName(), actualFrame.get("C"));
            assertEquals(expectedFrame.getMethodName(), actualFrame.get("M"));
            assertEquals(expectedFrame.getFileName(), actualFrame.get("F"));
            if (expectedFrame.getLineNumber() >= 0) {
                assertEquals(expectedFrame.getLineNumber(), actualFrame.get("L"));
            }
        }
    }
    
    
}
