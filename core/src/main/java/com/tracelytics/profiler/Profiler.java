package com.tracelytics.profiler;

import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.Event;
import com.tracelytics.joboe.EventReporter;
import com.tracelytics.joboe.Metadata;
import com.tracelytics.joboe.config.ProfilerSetting;
import com.tracelytics.joboe.settings.SettingsArg;
import com.tracelytics.joboe.settings.SettingsArgChangeListener;
import com.tracelytics.joboe.settings.SettingsManager;
import com.tracelytics.logging.Logger;
import com.tracelytics.logging.LoggerFactory;
import com.tracelytics.util.DaemonThreadFactory;
import com.tracelytics.util.TimeUtils;

import java.util.*;
import java.util.concurrent.*;

/**
 * A Sampling Profiler that runs a single background thread to take stack trace snapshots of a list of "tracked threads" on a given interval
 * 
 * The list of "tracked threads" are set by other external logic and is not controlled by this profiler 
 *  
 * @author pluk
 *
 */
public class Profiler {
    private static ConcurrentMap<Long, Profile> profileByTraceId = new ConcurrentHashMap<Long, Profiler.Profile>(); //for quicker lookup
    private static final String APPOPTICS_THREAD_PREFIX = "AppOptics-";
    
    private static Logger logger = LoggerFactory.getLogger();
    
    public static EventReporter reporter;
    
    private static Status status = Status.UNINITIALIZED;
    
    public enum Status { UNINITIALIZED, PAUSED_CIRCUIT_BREAKER, RUNNING, STOPPING, STOPPED }
    private static long interval; //current interval to take snapshots on, could be changed dynamically  
    
    private static ProfilerSetting localSetting; //profiler settings from local config
    private static Future<?> samplerFuture; 
    
    static final int MAX_REPORTED_FRAME_DEPTH = 400;

    /**
     * Listens to interval changes beamed down by collector
     */
    private static void addIntervalChangeListener() {
        SettingsManager.registerListener(new SettingsArgChangeListener<Integer>(SettingsArg.PROFILING_INTERVAL) {
            @Override
            public void onChange(Integer newValue) {
                logger.info("Collector sends new profiling interval : " + (newValue != null ? newValue.toString() : "(empty)"));
                if (newValue != null) { //value from collector also has higher precedence than local settings
                    interval = newValue;
                } else {
                    interval = localSetting.getInterval();
                }

                logger.info("Updated profiling interval to : " + interval);
                
                if (interval <= 0) { //stop profiler if new value is negative
                    if (status != Status.STOPPED) { //only if profiler is not stopped yet
                        logger.info("Profiler stopping after interval update from remote collector, previous status: " + status);
                        stop();
                    }
                } else { //otherwise start profiler if it was stopped
                    if (status == Status.STOPPED) { 
                        logger.info("Profiler starting after interval update from remote collector");
                        start();
                    }
                }
            }
        });
    }

    /**
     * Initializes profiler by providing the ProfilerSetting. The Profiler will start listening for remote config changes
     * 
     * The agent might be put into either standby mode (if `interval` is 0) or running mode if `interval` is valid
     * 
     * Ignores calls if profiler is NOT in `UNINITIALIZED` state
     * @param setting
     * @param reporter  reporter used to export the captured data (in {@link Event} output format)
     */
    public static void initialize(ProfilerSetting setting, EventReporter reporter) {
        if (status == Status.UNINITIALIZED) {
            localSetting = setting;
            interval = localSetting.getInterval();
            
            status = Status.STOPPED; //switch to stopped as the profiler is going to be initialized but has not started profiling yet
            
            Profiler.reporter = reporter;

            if (interval != 0) {
                logger.debug("Starting profiler worker, previous status: " + status);
                start();
            } else {
                logger.debug("No profiling started. Profiler is on standby, previous status: " + status);
            }
            
            addIntervalChangeListener(); //add listener here to avoid race condition on starting the profiler
        } else {
            logger.info("Profiler is already initialized, ignoring initialize operation");
        }
    }
    
    private static void start() {
        if (status == Status.STOPPED) {
            run();
        } else {
            logger.warn("Cannot start a profiler when it's in status " + status);
        }
    }
    
    /**
     * Starts the background thread that takes snapshots on `interval`
     */
    static void run() {
        status = Status.RUNNING;
        
        final CircuitBreaker circuitBreaker = new CircuitBreaker(localSetting.getCircuitBreakerDurationThreshold(), localSetting.getCircuitBreakerCountThreshold());
        
        ExecutorService service = Executors.newFixedThreadPool(1, DaemonThreadFactory.newInstance("profiling-sampler"));
        samplerFuture = service.submit(new Runnable() {
            public void run() {
                while (status != Status.STOPPING) {
                    status = Status.RUNNING;
                    try {
                        ProfilingDurationInfo durationInfo = checkThreads(); //take and report snapshots on the list on tracked threads
                        long duration = durationInfo.duration;

                        long circuitBreakerPause = circuitBreaker.getPause(duration); //consult with the circuit break on whether the last operation would trigger a pause
                        if (circuitBreakerPause > 0) { //circuit breaker is triggered, pausing
                            status = Status.PAUSED_CIRCUIT_BREAKER;
                            logger.info("Pause profiling for " + circuitBreakerPause + " secs. Previous profiling operation took " + duration + "ms. That's total of " +  circuitBreaker.getBreakCountThreshold() +  " consecutive profiling operation(s) that exceeded the circuit breaker duration threshold " + circuitBreaker.getBreakDurationThreshold() + " ms");
                            TimeUnit.SECONDS.sleep(circuitBreakerPause);
                        } else {
                            long sleepTime = interval - System.currentTimeMillis() % interval; //snap the sleep time to the next closest time frame based on the interval

                            TimeUnit.MILLISECONDS.sleep(sleepTime);
                        }
                    } catch (InterruptedException e) {
                        logger.debug("Profiler interrupted: " + e.getMessage()); //hard to tell whether this is triggered by JVM shutdown
                        status = Status.STOPPING; //flag it to stop
                    } catch (Throwable e) {
                        logger.warn("Profiler interrupted unexpectedly: " + e.getMessage(), e);
                        status = Status.STOPPING; //flag it to stop
                    }
                }
                status = Status.STOPPED;
            }
        });
        service.shutdown();
    }
    
    /**
     * Stops the background thread that takes snapshots on `interval`, blocks until the thread is dead
     */
    public static void stop() {
        logger.info("Stopping Profiler");
        status = Status.STOPPING; //flag that the profiler is signaled for stopping
        if (samplerFuture != null) {
            samplerFuture.cancel(true);
        }
        logger.info("Profiler is stopped");
    }
    
    /**
     * Takes and reports snapshots on the tracked threads
     * @return the duration of the checkThreads operation
     */
    private static ProfilingDurationInfo checkThreads() {
        if (profileByTraceId.isEmpty()) {
            return new ProfilingDurationInfo(-1, Collections.EMPTY_LIST);
        }
        
        long start = System.currentTimeMillis();

        List<String> taskIds = new ArrayList<String>();
        for (Profile profile : new HashSet<Profile>(profileByTraceId.values())) {
            for (Thread thread : profile.getActiveThreads()) {
                long snapshotTimestamp = TimeUtils.getTimestampMicroSeconds();
                StackTraceElement[] stackTrace = thread.getStackTrace();
                profile.record(thread, stackTrace, snapshotTimestamp);
            }
            for (SnapshotTracker tracker : profile.snapshotTrackersByThread.values()) {
                taskIds.add(tracker.metadata.taskHexString());
            }

        }
        long end = System.currentTimeMillis();
        long duration = end - start;


        //return duration;
        return new ProfilingDurationInfo(duration, taskIds);
    }

    private static class ProfilingDurationInfo {
        private long duration;
        private List<String> taskIds;

        private ProfilingDurationInfo(long duration, List<String> taskIds) {
            this.duration = duration;
            this.taskIds = taskIds;
        }
    }

    public static Status getStatus() {
        return status;
    }

    /**
     * Adds a thread to be tracked for profiling
     * @param thread
     * @param metadata
     * @param traceId
     * @return
     */
    public static boolean addProfiledThread(Thread thread, Metadata metadata, long traceId) {
        if (thread.getName() != null && thread.getName().startsWith(APPOPTICS_THREAD_PREFIX)) { //do not instrument our own threads
            return false;
        }

        if (status != Status.RUNNING) {
            logger.debug("Add profile thread operation skipped as profiler is not running, status : " + status);
            return false;
        }
        
        Profile profile;
        profile = profileByTraceId.get(traceId);
        if (profile == null) { //then this task is instrumented the first time, add profile
            profile = new Profile();
            profileByTraceId.put(traceId, profile);
        }
        
        if (profile.startProfilingOnThread(thread, metadata)) {
            logger.debug("Started profiling on Thread id: " + thread.getId() + " name: " + thread.getName() + " for trace: " + traceId);
            return true;
        } else {
            return false;
        }
    }
    
    /**
     * Stops profiling on all threads triggered by this parent (tracing) span
     * @param traceId
     * @return
     */
    public static Profile stopProfile(long traceId) {
        Profile profile = profileByTraceId.remove(traceId);
        
        if (profile != null) {
            profile.stop();
        }
        
        return profile;
    }
    
    /**
     * Stops profiling on a particular thread
     * @param profiledThread
     * @param traceId
     */
    public static void removeProfiledThread(Thread profiledThread, long traceId) {
        Profile profile = profileByTraceId.get(traceId);
        if (profile != null) {
            if (profile.stopProfilingOnThread(profiledThread)) {
                logger.debug("Stopped profiling on Thread id: " + profiledThread.getId() + " name: " + profiledThread.getName() + " for trace " + traceId);
            }
        }
    }

    /**
     * A Profile is created per trace that has profiling triggered. It keeps a map of threads being profiled.
     * 
     * For example if a servlet triggers profiling, a `Profile` instance would be created for that. 
     * 
     * If later on more threads related to the same servlet call are tracked, they will be added to this same Profile instance.
     * 
     * 
     * @author pluk
     *
     */
    public static class Profile {
        private Map<Thread, SnapshotTracker> snapshotTrackersByThread = new ConcurrentHashMap<Thread, SnapshotTracker>();
        private final ProfilerSetting setting; 
        
        private Profile() {
            this(Profiler.localSetting);
        }
        
        Profile(ProfilerSetting setting) {
            this.setting = setting;
        }
        
        /**
         * Creates an "entry" event for the profiling span with a "SpanRef" pointing back to the parent "tracing" span that triggers/create this profile
         * @param parentMetadata
         * @return
         */
        private Metadata createProfileSpanEntry(Metadata parentMetadata) {
            Metadata entryMetadata = new Metadata(parentMetadata);

            Event snapshotEntry = Context.createEventWithContext(entryMetadata, false);
            snapshotEntry.addInfo("Label", "entry",
                                  "Spec", "profiling",
            //                      Constants.XTR_EDGE_KEY, parentSpan.context().getMetadata().opHexString(), //for now, easier to see trace in front-end
                                  "Language", "java",
                                  "Interval", (int) interval,
                                  "SpanRef", parentMetadata.opHexString());


            snapshotEntry.report(entryMetadata, Profiler.reporter);

            return entryMetadata;
        }
        
        public void stop() {
            for (SnapshotTracker tracker : snapshotTrackersByThread.values()) {
                tracker.stop();
                createProfileSpanExit(tracker);
            }
            snapshotTrackersByThread.clear();
        }
        
        /**
         * Records and reports (if not omitted) the stack trace provided by in the parameters 
         * @param thread
         * @param stack
         * @param collectionTime    time in microseconde when a snapshot was collected
         */
        public void record(Thread thread, StackTraceElement[] stack, long collectionTime) {
            long threadId = thread.getId();
            SnapshotTracker tracker = snapshotTrackersByThread.get(thread);  
            int framesExited;
            
            StackTraceElement[] newFrames;
            
            int originalFramesCount = stack.length; //get the framesCount before trimming
            stack = trimStack(stack);
            
            if (tracker != null && !tracker.stopped) {
                if (tracker.stack == null) {
                    framesExited = 0;
                    newFrames = stack;
                } else {
                    //start matching previous stack with current stack, from the back of the list (bottom of calling stack frame) to front (top stack frame) 
                    int currentCallFrameWalker = stack.length - 1;
                    int previousCallFrameWalker = tracker.stack.length - 1;
                    while (previousCallFrameWalker >= 0 && currentCallFrameWalker >= 0) {
                        StackTraceElement previousCallFrame = tracker.stack[previousCallFrameWalker];
                        StackTraceElement currentCallFrame = stack[currentCallFrameWalker];
                        if (!previousCallFrame.equals(currentCallFrame)) { //diverges, exit here and count frames pop
                            break;
                        }
                        
                        currentCallFrameWalker --;
                        previousCallFrameWalker --;
                    }
                    
                    framesExited = previousCallFrameWalker + 1;
                    if (currentCallFrameWalker >= 0) {
                        newFrames = Arrays.copyOfRange(stack, 0, currentCallFrameWalker + 1);
                    } else {
                        newFrames = null;
                    }
                }
                
                if (newFrames != null || framesExited > 0) { //only update and report if things have changed
                    synchronized(tracker) {
                        if (!tracker.stopped) {
                            reportSnapshot(tracker.metadata, framesExited, tracker.snapshotsOmitted.isEmpty() ? Collections.EMPTY_LIST : new ArrayList<Long>(tracker.snapshotsOmitted),  newFrames, originalFramesCount, threadId, collectionTime);
                        }
                    }
                    tracker.stack = stack;
                    tracker.snapshotsOmitted.clear(); //reset snapshots omitted
                } else {
                    tracker.snapshotsOmitted.add(collectionTime);
                }
            }
        }
        
        /**
         * Trim the stack by removing top frames that matches ProfilerSetting.getExcludePackages or if it's deeper than MAX_REPORTED_FRAME_DEPTH 
         * @param stack
         * @return
         */
        private StackTraceElement[] trimStack(StackTraceElement[] stack) {
            StackTraceElement[] trimmedStack;
            if (stack.length > MAX_REPORTED_FRAME_DEPTH) {
                trimmedStack = Arrays.copyOfRange(stack, stack.length - MAX_REPORTED_FRAME_DEPTH, stack.length);
            } else {
                trimmedStack = stack;
            }
            
            if (setting.getExcludePackages().isEmpty()) { //no trimming required
                return trimmedStack;
            }
            //traverse from top to bottom
            for (int i = 0; i < trimmedStack.length; i ++) {
                StackTraceElement frame = trimmedStack[i];
                boolean isExcludedFrame = false;
                for (String excludePackage : setting.getExcludePackages()) {
                    String frameClassName = frame.getClassName();
                    if (frameClassName.startsWith(excludePackage + ".")) {
                        isExcludedFrame = true;
                        break;
                    }
                }
                
                if (!isExcludedFrame) { //this current frame does not match any of the exclude prefix, that means the rest of this stack should be reported
                    if (i == 0) {
                        return trimmedStack; //no change
                    } else {
                        return Arrays.copyOfRange(trimmedStack, i, trimmedStack.length);
                    }
                }
            }
            
            return new StackTraceElement[0]; //everything is excluded...
            
        }

        /**
         * Starts profiling on a thread with the parent (tracing) span
         * @param thread
         * @param parentMetadata
         * @return
         */
        boolean startProfilingOnThread(Thread thread, Metadata parentMetadata) {
            if (!snapshotTrackersByThread.containsKey(thread)) {
                Metadata snapshotMetadata = createProfileSpanEntry(parentMetadata);
                SnapshotTracker tracker = new SnapshotTracker(snapshotMetadata);
                snapshotTrackersByThread.put(thread, tracker);
                return true;
            } else { //this thread is already tracked
                return false;
            }
        }
        
        /**
         * Stops profiling on this particular thread
         * @param thread
         * @return
         */
        boolean stopProfilingOnThread(Thread thread) {
            SnapshotTracker tracker = snapshotTrackersByThread.remove(thread);
            if (tracker != null) {
                tracker.stop();
                createProfileSpanExit(tracker);
            }
            return tracker != null;
        }
        
        private void createProfileSpanExit(SnapshotTracker tracker) {
            Event snapshotExit = Context.createEventWithContext(tracker.metadata);
            snapshotExit.addInfo("Label", "exit",
                                 "Spec", "profiling",
                                 "SnapshotsOmitted", tracker.snapshotsOmitted);
            
            synchronized(tracker) {
                snapshotExit.addEdge(tracker.metadata);
                snapshotExit.report(tracker.metadata, Profiler.reporter);
            }
            
        }
        
        /**
         * Get a list of threads currently tracked by this Profile
         * @return
         */
        public Set<Thread> getActiveThreads() {
            return new HashSet<Thread>(snapshotTrackersByThread.keySet());
        }
        
        private void reportSnapshot(Metadata metadata, int framesExited, List<Long> snapshotsOmitted, StackTraceElement[] newFrames, int framesCount, long threadId, long timestamp) {
            Event event;
            
            event = Context.createEventWithContext(metadata);
            event.addInfo("Label", "info",
                          "Spec", "profiling",
                          "FramesExited", framesExited,
                          "SnapshotsOmitted", snapshotsOmitted,
                          "FramesCount", framesCount);     
            event.setTimestamp(timestamp);
            event.setThreadId(threadId);
            
            if (newFrames != null) {
                List<Map<String, Object>> newFramesValue = new ArrayList<Map<String, Object>>();
                for (StackTraceElement newFrame : newFrames) {
                    Map<String, Object> frameKeyValues = new HashMap<String, Object>();
                    String className = newFrame.getClassName();
                    if (className != null) {
                        frameKeyValues.put("C", className);
                    } 
                    String fileName = newFrame.getFileName();
                    if (fileName != null) {
                        frameKeyValues.put("F", fileName);
                    }
                    int lineNumber = newFrame.getLineNumber();
                    if (lineNumber > 0) {
                        frameKeyValues.put("L", lineNumber);
                    }
                    String methodName = newFrame.getMethodName();
                    if (methodName != null) {
                        frameKeyValues.put("M", methodName);
                    }
                    newFramesValue.add(frameKeyValues);
                }
                
                event.addInfo("NewFrames", newFramesValue);
            }            
            
            event.report(metadata, reporter);
            
        }
    }
    
    /**
     * Keeps the state of tracked thread to enable snapshot reporting on a thread.
     * 
     * State is important as: 
     * 1. Enables the check if the current snapshot can be omitted if it's identical to the previously reported one
     * 2. Enables synchronization to avoid reporting snapshots if the thread (or it's parent span) is flagged to stop profiling
     * @author pluk
     *
     */
    public static class SnapshotTracker {
        private StackTraceElement[] stack;
        private final Metadata metadata;
        private boolean stopped = false;
        private ArrayList<Long> snapshotsOmitted = new ArrayList<Long>();
        
        public SnapshotTracker(Metadata metadata) {
            this.metadata = metadata;
        }
        
        private void stop() {
            stopped  = true;
        }
    }
    
    /**
     * A stateful circuit breaker that acts on consecutive calls to `getPause` when the duration parameter is a above or below the `durationThreshold`
     * 
     * The goal of this stateful circuit breaker is to break when the operation is consistently slow and increase the pause time exponentially (up to a max) if the system remains slow
     * 
     * However, if the system resumes back to normal, it should reset the pause time.
     * 
     * @author pluk
     *
     */
    static class CircuitBreaker {
        private int consecutiveBadCount = 0;
        private int consecutiveGoodCount = 0;
        private final int durationThreshold;
        private final int countThreshold;
        static final int INITIAL_CIRCUIT_BREAKER_PAUSE = 60;
        static final int MAX_CIRCUIT_BREAKER_PAUSE= 60 * 60;
        static final double PAUSE_MULTIPLIER = 1.5;
        private int nextPause = INITIAL_CIRCUIT_BREAKER_PAUSE;
        
        CircuitBreaker(int durationThreshold, int countThreshold) {
            this.durationThreshold = durationThreshold;
            this.countThreshold = countThreshold;
        }
        
        /**
         * This might mutate the current circuit breaker states, depending on the current state and the duration parameters: 
         * 
         * At first the circuit breaker starts with a "Normal" state
         * When there are n (defined by `countThreshold`) consecutive `getPause` calls with param `duration` above the `durationThreshold`, the circuit breaker will go into the "Break" state
         * "Break" state will be transitioned into a "Restored but broken recently" state when there's a new `getPause` call 
         * "Restored but broken recently" state will be transitioned to "Normal" state if there are n consecutive `getPause` calls with param `duration` below or equal to the `durationThreshold`
         * 
         * And below are the behaviors of this method in various states/transitions:
         *  
         * When transition to "Normal" state, `nextPause` is set to INITIAL_CIRCUIT_BREAKER_PAUSE
         * When in "Normal" state, `getPause` returns 0
         * When transition to "Break" state, `getPause` returns `nextPause` then `nextPause` is multiplied by `PAUSE_MULTIPLIER`
         * When transition to or in "Restored but broken recently" state, `getPause` returns 0 
         * 
         * @param duration
         * @return
         */
        public long getPause(long duration) {
            int pause = 0;
            if (duration <= durationThreshold) {
                if (consecutiveGoodCount < countThreshold) {
                    consecutiveGoodCount ++;
                    if (consecutiveGoodCount == countThreshold) {
                        nextPause = INITIAL_CIRCUIT_BREAKER_PAUSE; //reset pause as duration is good for the last countThreshold consecutive operations
                    }
                    consecutiveBadCount = 0; //reset consecutive bad count
                }
            } else {
                if (consecutiveBadCount < countThreshold) {
                    consecutiveBadCount ++;
                    if (consecutiveBadCount == countThreshold) { //trigger circuit breaker
                        pause = nextPause;
                        
                        nextPause *= PAUSE_MULTIPLIER;
                        nextPause = Math.min(nextPause, MAX_CIRCUIT_BREAKER_PAUSE);
                        consecutiveBadCount = 0; //also reset the bad count, if we get more consecutive bad duration, we want to increase the threshold further
                    }
                    consecutiveGoodCount = 0; //reset consecutive good count
                }
            }
            return pause;
            
            
        }

        public int getBreakDurationThreshold() {
            return durationThreshold;
        }
        
        public int getBreakCountThreshold() {
            return countThreshold;
        }
    }
}
