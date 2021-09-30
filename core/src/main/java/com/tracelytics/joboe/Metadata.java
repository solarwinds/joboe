package com.tracelytics.joboe;

import static com.tracelytics.joboe.Constants.MASK_VERSION;
import static com.tracelytics.joboe.Constants.MAX_METADATA_PACK_LEN;
import static com.tracelytics.joboe.Constants.OP_ID_LEN;
import static com.tracelytics.joboe.Constants.TASK_ID_LEN;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import com.tracelytics.ext.uncommons.maths.random.SecureRandomSeedGenerator;
import com.tracelytics.ext.uncommons.maths.random.SeedException;
import com.tracelytics.ext.uncommons.maths.random.XORShiftRNG;
import com.tracelytics.joboe.config.ConfigManager;
import com.tracelytics.joboe.config.ConfigProperty;
import com.tracelytics.joboe.settings.SettingsArg;
import com.tracelytics.joboe.settings.SettingsArgChangeListener;
import com.tracelytics.joboe.settings.SettingsManager;
import com.tracelytics.logging.Logger;
import com.tracelytics.logging.LoggerFactory;

/**
 * Oboe Metadata: Task and Op IDs
 * Note that this is migrated from AO's X-Trace ID and it complies with the W3C trace context spec: https://www.w3.org/TR/trace-context

 */
public class Metadata {
    private static final Logger logger = LoggerFactory.getLogger();

    private byte taskID[];
    private byte opID[];
    
    private int taskLen = TASK_ID_LEN;
    private int opLen = OP_ID_LEN;
    
    private byte flags;
    
    private boolean isAsync;

    public static final char[] hexTable = {'0','1','2','3','4','5','6','7','8','9','a','b','c','d','e','f'};
    public static final byte[] unsetTaskID = new byte[TASK_ID_LEN]; // initialized to zero
    public static final byte[] unsetOpID = new byte[OP_ID_LEN]; // initialized to zero
    
    private static int ttl; //in millisec
    public static final int DEFAULT_TTL = 20 * 60 * 1000; //20 mins by default, in unit of millisec
    private static int maxEvents;
    private static int maxBacktraces;
    public static final int DEFAULT_MAX_EVENTS = 100000; // max 100k events per trace by default
    public static final int DEFAULT_MAX_BACKTRACES = 1000; // max 1000 backtraces per trace by default
    
    public static final int CURRENT_VERSION = 0; // current W3C trace context version as of 2021
    
    private long creationTimestamp; //creation timestamp in millisec
    private Long traceId;
    private AtomicInteger numEvents = new AtomicInteger();
    private AtomicInteger numBacktraces = new AtomicInteger();
    private boolean reportMetrics = false;

    static {
        Integer configTtlInSecond = (Integer) ConfigManager.getConfig(ConfigProperty.AGENT_CONTEXT_TTL);
        ttl = configTtlInSecond != null ? configTtlInSecond * 1000 : DEFAULT_TTL; //convert to millisec

        Integer configMaxEvents = (Integer) ConfigManager.getConfig(ConfigProperty.AGENT_CONTEXT_MAX_EVENTS);
        maxEvents = configMaxEvents != null ? configMaxEvents : DEFAULT_MAX_EVENTS;
        
        Integer configMaxBacktraces = (Integer) ConfigManager.getConfig(ConfigProperty.AGENT_CONTEXT_MAX_BACKTRACES);
        maxBacktraces = configMaxBacktraces != null ? configMaxBacktraces : DEFAULT_MAX_BACKTRACES;

        addTtlChangeListener();
        addMaxEventsChangeListener();
        addMaxBacktracesChangeListener();
    }
    
    public Metadata() {
        initialize();
    }

    public Metadata(String hexStr) throws OboeException {
        initialize();
        fromHexString(hexStr);
    }

    public Metadata(Metadata toClone) {
        if (toClone.isExpired(System.currentTimeMillis())) { //stop this source from spreading expired metadata
            toClone.invalidate();
        }

        initialize();
        this.taskLen = toClone.taskLen;
        this.opLen = toClone.opLen;
        this.isAsync = toClone.isAsync;
        this.creationTimestamp = toClone.creationTimestamp;
        this.numEvents = toClone.numEvents; //use the same instance of numEvent as we want to keep a centralized counter for all clones
        this.numBacktraces = toClone.numBacktraces; //use the same instance of numBacktraces as we want to keep a centralized counter for all clones
        System.arraycopy(toClone.taskID, 0, this.taskID, 0, TASK_ID_LEN);
        System.arraycopy(toClone.opID, 0, this.opID, 0, OP_ID_LEN);
        this.flags = toClone.flags;
        this.traceId = toClone.traceId;
        this.reportMetrics = toClone.reportMetrics;
    }
    
    /**
     * Listens to dynamic ttl change from Settings
     */
    private static void addTtlChangeListener() {
        SettingsManager.registerListener(new SettingsArgChangeListener<Integer>(SettingsArg.MAX_CONTEXT_AGE) {
            @Override
            public void onChange(Integer newValue) {
                if (newValue != null) {
                    ttl = newValue * 1000; //convert from seconds to milliseconds
                } else { //reset back to default
                    ttl = DEFAULT_TTL;
                }
            }
        });
    }

    private static void addMaxEventsChangeListener() {
        SettingsManager.registerListener(new SettingsArgChangeListener<Integer>(SettingsArg.MAX_CONTEXT_EVENTS) {
            @Override
            public void onChange(Integer newValue) {
                maxEvents = (newValue != null) ? newValue : DEFAULT_MAX_EVENTS;
            }
        });
    }
    
    private static void addMaxBacktracesChangeListener() {
        SettingsManager.registerListener(new SettingsArgChangeListener<Integer>(SettingsArg.MAX_CONTEXT_BACKTRACES) {
            @Override
            public void onChange(Integer newValue) {
                maxBacktraces = (newValue != null) ? newValue : DEFAULT_MAX_BACKTRACES;
            }
        });
    }

    
    /** Clears Task and Op IDs */
    public void initialize() {
        taskID = new byte[TASK_ID_LEN];
        taskLen = TASK_ID_LEN;
        opID = new byte[OP_ID_LEN];
        opLen = OP_ID_LEN;
        flags = 0x0;
        isAsync = false;
        traceId = null;
        numEvents.set(0);
        numBacktraces.set(0);
        creationTimestamp = System.currentTimeMillis();
        reportMetrics = false;
    }
    
    /**
     * Invalidates this metadata. For now, it will just set all the bits back to zeros
     */
    public void invalidate() {
        initialize();
    }

    public void randomize() {
        randomize(true);
    }
    
    /**
     * Randomizes and resets the state of this Metadata 
     */
    public void randomize(boolean isSampled) {
        initialize();
        randomizeTaskID();
        if (isSampled) {
            randomizeOpID();
        }
        setSampled(isSampled);
    }

    public void randomizeTaskID() { 
        random.nextBytes(taskID);
        //just in case if it really generates all zeros, then flip the last byte to a non zero value
        if (!isValid()) {
            taskID[taskLen - 1] = 0x1;
        }
    }

    public void randomizeOpID() {
        random.nextBytes(opID);
    }

    public void setOpID(Metadata md) {
        this.opLen = md.opLen;
        setOpID(md.opID);
    }

    public void setOpID(String opId) throws OboeException {
        setOpID(hexToBytes(opId));
    }

    public void setOpID(byte[] opId) {
        System.arraycopy(opId, 0, this.opID, 0, OP_ID_LEN);
    }
    
    /**
     * Whether the metadata has a valid task id - the operation has gone through a valid entry point (might or might not be sampled)
     * @return
     */
    public boolean isValid() {
        return !Arrays.equals(taskID, unsetTaskID);
    }
    
    /**
     * Whether the metadata has SAMPLED flag turned on - the operation is sampled to generate tracing events 
     * @return
     */
    public boolean isSampled() {
        return getFlag(Flag.SAMPLED); 
    }
    
    public boolean getFlag(Flag flag) {
        return (flags & flag.mask) == flag.mask; 
    }

    public void setSampled(boolean sampled) {
        setFlag(Flag.SAMPLED, sampled);
    }
    
    public void setFlag(Flag flag, boolean value) {
        if (value) {
            flags |= flag.mask;
        } else {
            flags &= (~flag.mask);
        }
    }

    /**
     * Increase the event counter tracking the number of events for this trace by 1
     * and return whether the counter is within valid limits after the increment 
     * 
     * The event counter persists across copies of this Metadata (used in the same trace).
     * 
     * @return whether the counter is within valid limits after the increment
     */
    public boolean incrNumEvents() {
        int currentCount = numEvents.incrementAndGet();
        if (currentCount == maxEvents + 1) { //only report it once on first limit exceeded
            logger.info("Exceeded maximum number of events allowed per trace [" + maxEvents + "] for task ID [" + taskHexString() + "]");
        }
        return currentCount <= maxEvents;
    }
    
    /**
     * Increase the backtrace counter tracking the number of backtraces for this trace by 1
     * and return whether the counter is within valid limits after the increment 
     * 
     * The event counter persists across copies of this Metadata (used in the same trace).
     * 
     * @return whether the counter is within valid limits after the increment
     */
    public boolean incrNumBacktraces() {
        int currentCount = numBacktraces.incrementAndGet();
        if (currentCount == maxBacktraces + 1) { //only report it once on first limit exceeded
            logger.info("Exceeded maximum number of backtraces allowed per trace [" + maxBacktraces + "] for task ID [" + taskHexString() + "]");
        }
        return currentCount <= maxBacktraces;
    }

    /**
     * 
     * @param reportingTimestamp    the timestamp to be reported in millisec
     * @return
     */
    public boolean isExpired(long reportingTimestamp) {
        if (isValid()) {
            boolean expired = reportingTimestamp - creationTimestamp > ttl;
            if (expired) {
                logger.info("Context of " + toHexString() + " has been expired");
            }
            return expired;
        } else { //default invalid context never expires
            return false;
        }
    }
    
    public boolean isTaskEqual(Metadata md) {
        return Arrays.equals(taskID, md.taskID);
    }

    public boolean isOpEqual(Metadata md) {
        return Arrays.equals(opID, md.opID);
    }

    /**  Packs metadata into byte buffer */
    // TODO: check pack
    public byte[] getPackedMetadata(int version) {
        int bufSize = 1 + taskLen + opLen + 1; //header byte, task ID, op ID, flags
        byte buf[] = new byte[bufSize];

        int taskBits = (taskLen >> 2) - 1;

        // Header with version and lengths:
        buf[0] = (byte) (version << 4);
        buf[0] |= (taskBits == 4) ? 3 : taskBits;
        buf[0] |= ((opLen >> 2) - 1) << 3;

        // Task and Op ID data:
        int writeMarker = 1;
        buf[writeMarker++] = '-';

        System.arraycopy(taskID, 0, buf, writeMarker, taskLen);
        writeMarker += taskLen;

        buf[writeMarker++] = '-';

        System.arraycopy(opID, 0, buf, writeMarker, opLen);
        writeMarker += opLen;

        buf[writeMarker++] = '-';

        buf[writeMarker] = flags;        

        return buf;
    }

    /** Populates this object from packed metadata contained in the byte buffer */
    // TODO: check unpack
    public void unpackMetadata(byte buf[])
        throws OboeException {

        if (buf == null || buf.length < 1) {
            throw new OboeException("Byte buffer is not valid");
        }

        byte header = buf[0];
        int version = (header & MASK_VERSION) >> 4; 
        if (version  != CURRENT_VERSION) {
            throw new OboeException("Unexpected version. Found " + version + " but expected " + CURRENT_VERSION);
        }

        int expectedLen = 1 + TASK_ID_LEN + OP_ID_LEN + 1 + 3; //header + task id + op id + flags + delimiters
        if (buf.length < expectedLen ) {
            throw new OboeException("Invalid buffer length: expected " + expectedLen);
        }

        int readMarker = 2; // the version and the delimiter
        System.arraycopy(buf, readMarker, taskID, 0, taskLen);
        readMarker += taskLen;
        readMarker++; // the '-' delimiter
        System.arraycopy(buf, readMarker, opID, 0, opLen);
        readMarker += opLen;
        readMarker++; // the '-' delimiter
        this.flags = buf[readMarker];
        
        this.creationTimestamp =  System.currentTimeMillis(); //a new task id, consider this a new metadata
    }

    /** Returns hex representation of this metadata instance */
    public String toHexString() {
        return bytesToHex(getPackedMetadata(CURRENT_VERSION));
    }
    
    @Override
    public String toString() {
        return toHexString();
    }
    
    /**
     * Gets a hex string representation by setting an explicit version number. For internal use only
     * @param versionOverride
     * @return
     */
    public String toHexString(int versionOverride) {
        return bytesToHex(getPackedMetadata(versionOverride));
    }

    public byte[] getOpID() {
        return opID;
    }

    public byte[] getTaskID() {
        return taskID;
    }

    public String opHexString() {
        return bytesToHex(opID, opLen);
    }
    
    public String taskHexString() {
        return bytesToHex(taskID, taskLen);
    }

    /** Populates this metadata instance from a hex string */
    public void fromHexString(String s)
        throws OboeException {
        unpackMetadata(hexToBytes(s));
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + flags;
        result = prime * result + (isAsync ? 1231 : 1237);
        result = prime * result + Arrays.hashCode(opID);
        result = prime * result + opLen;
        result = prime * result + Arrays.hashCode(taskID);
        result = prime * result + taskLen;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Metadata other = (Metadata) obj;
        if (flags != other.flags)
            return false;
        if (isAsync != other.isAsync)
            return false;
        if (!Arrays.equals(opID, other.opID))
            return false;
        if (opLen != other.opLen)
            return false;
        if (!Arrays.equals(taskID, other.taskID))
            return false;
        if (taskLen != other.taskLen)
            return false;
        return true;
    }

    private String bytesToHex(byte[] bytes) {
        return bytesToHex(bytes, bytes.length);
    }

    // TODO:
    private String bytesToHex(byte[] bytes, int len) {
        char[] hexChars = new char[len * 2];
        int v;
        for (int j = 0; j < len; j++) {
            v = bytes[j] & 0xFF;

            if (v == '-') {
                hexChars[j++] = '-';
                continue;
            }
            hexChars[j*2] = hexTable[v/16];
            hexChars[j*2 + 1] = hexTable[v%16];
        }
        return new String(hexChars);
    }

    // TODO
    private byte[] hexToBytes(String s)
            throws OboeException {
        int len = s.length();

        if ((len % 2) != 0 || len > MAX_METADATA_PACK_LEN) {
            throw new OboeException("Invalid string length");
        }

        byte[] buf = new byte[len / 2];
        for (int i = 0, j = 0; i < len; j++) {
            if (s.charAt(i) == '-') {
                buf[j] = '-';
                i++;
                continue;
            }
            buf[j] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
            i += 2;
        }
        return buf;
    }
    
    public boolean isAsync() {
        return isAsync;
    }

    public void setIsAsync(boolean isAsync) {
        this.isAsync = isAsync;
    }
    
    public void setTraceId(Long traceId) {
        this.traceId = traceId;
    }
    
    public Long getTraceId() {
        return traceId;
    }
   
    public void setReportMetrics(boolean reportMetrics) {
        this.reportMetrics = reportMetrics;
    }
    
    public boolean isReportMetrics() {
        return reportMetrics;
    }
    
    /**
     * Checks if the xTraceId is compatible with this current agent  
     * @param xTraceId
     * @return
     * @exception NullPointerException if xTraceId is null
     */
    public static boolean isCompatible(String xTraceId) {
        try {
            new Metadata(xTraceId);
            return true;
        } catch (OboeException e) {
            logger.debug("X-Trace ID [" + xTraceId + "] not compatible : " + e.getMessage());
            return false;
        }
    }
    
    /**
     * asyncLayerLevel is used to track layerLevel for Metadata that is marked as Async. 
     * 
     * This is used to determine whether the current level of an extent within an async stack (indicated by the isAsync flag of the Metadata instance) is a top level extent, 
     * such that it's eligible to be flagged as async. 
     * 
     * More details at 
     * 
     * Take note that this approach would not work if the entry and exit events do not use the same Metadata instance
     * (for example exit event restores the context using plain string that 
     * some instrumentation on asynchronous constructs that have entry and exit events are on different threads).
     *  
     * This is considered rare cases as even though we are trying to address Async flag here, this is not to be confused with the async
     * constructed itself. 
     * 
     * The async flag here addresses operations that runs on a separate thread (hence asynchronous relative to the
     * thread that spawns it), but the operations themselves start and end on the same thread usually. (as opposed to those async construct whose
     * entry and exit events are on different threads)
     * 
     * Take note that even if this flag is off balance (due to concerns above), it will have rather mild impact as:
     * <ol>
     *  <li>This only affect whether we flag a top level extent in the async call stack as asynchronous or not</li>
     *  <li>In multi-threaded trace, the metadata usually gets cloned in forked extents, so incorrect asyncLayerLevel should not pollute other threads nor traces</li>
     * </ol>
     */
    private int asyncLayerLevel = 0;
    
    public int incrementAndGetAsyncLayerLevel() {
        return ++ asyncLayerLevel;
    }
    public int decrementAndGetAsyncLayerLevel() {
        return -- asyncLayerLevel;
    }
    
    static int getMaxEvents() {
        return maxEvents;
    }
    
    static int getTtl() {
        return ttl;
    }
    
    static int getMaxBacktraces() {
        return maxBacktraces;
    }
    
	// Shared random number generator to avoid overhead:
    private static Random random = null;

    static {
        // This RNG implementation is MUCH (over 10x) faster than Java's built-in SecureRandom
        // See http://maths.uncommons.org/
        try {
            try {
                //try /dev/urandom first, as using SecureRandomSeedGenerator could trigger slow startup due to /dev/random blocking (entropy exhaustion)
                random = new XORShiftRNG(new DevURandomSeedGenerator());  
            } catch (SeedException e) {
                logger.debug("Failed to use /dev/urandom as seed generator. Error message : " + e.getMessage());
                //try using the SecureRandomSeedGenerator instead
                random = new XORShiftRNG(new SecureRandomSeedGenerator());
            }
        } catch(Exception ex) {
            logger.error(ex.getMessage(), ex); // should never happen
        }
    }


    public String getCompactTraceId() {
        return taskHexString() + "-" + (isSampled() ? "1" : "0");
    }

    private enum Flag {
        SAMPLED((byte) 0x1);
        
        private final byte mask;
        
        private Flag(byte mask) {
            this.mask = mask;
        }
    }
    
}

