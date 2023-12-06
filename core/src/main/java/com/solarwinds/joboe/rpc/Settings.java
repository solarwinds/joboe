package com.solarwinds.joboe.rpc;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.solarwinds.joboe.TraceDecisionUtil;
import com.solarwinds.joboe.settings.SettingsArg;
import com.solarwinds.logging.Logger;
import com.solarwinds.logging.LoggerFactory;

/**
 * Settings extracted from RPC calls
 */
public class Settings extends com.solarwinds.joboe.settings.Settings {
    private static Logger logger = LoggerFactory.getLogger();
    
    
    private short type; // required
    private short flags; // required
    private long timestamp; // required, in millsec
    private long value; // required
    private String layer; // required
    private long ttl; //time to live this settings record 
    private Map<SettingsArg<?>, Object> args = new HashMap<SettingsArg<?>, Object>(); //other arguments
    
    public Settings(short type, String stringFlags, long timestamp, long value, long ttl, String layer, Map<String, ByteBuffer> args) {
        this.type = type;
        this.flags = convertFlagsFromStringToShort(stringFlags);
        this.timestamp = timestamp;
        if (value < 0) {
            logger.warn("Received invalid sample rate from RPC client : " + value + ", must be between 0 and " + TraceDecisionUtil.SAMPLE_RESOLUTION);
            this.value = 0;
        } else if (value > TraceDecisionUtil.SAMPLE_RESOLUTION) {
            logger.warn("Received invalid sample rate from RPC client : " + value + ", must be between 0 and " + TraceDecisionUtil.SAMPLE_RESOLUTION);
            this.value = TraceDecisionUtil.SAMPLE_RESOLUTION;
        } else {
            this.value = value;
        }
        this.ttl = ttl;
        this.layer = layer;
        readArgs(args);
    }

    /**
     * For internal testing purpose only
     * @param source
     * @param timestamp a new timestamp
     */
    public Settings(Settings source, long timestamp) {
        this.type = source.type;
        this.flags = source.flags;
        this.timestamp = timestamp; //take the new timestamp
        this.value = source.value;
        this.ttl = source.ttl;
        this.layer = source.layer;
        this.args.putAll(source.args);
    }
    
    /**
     * Read arguments from the input map
     * @param inputArgs
     */
    private void readArgs(Map<String, ByteBuffer> inputArgs) {
        for (Entry<String, ByteBuffer> inputArg : inputArgs.entrySet()) {
            SettingsArg<?> arg = SettingsArg.fromKey(inputArg.getKey());
            if (arg == null) {
                logger.debug("Cannot recognize argument [" + inputArg.getKey() + "], ignoring...");
            } else {
                args.put(arg, arg.readValue(inputArg.getValue()));
            }
        }
    }
    

    public static short convertFlagsFromStringToShort(String stringFlags) {
        short flags = 0;
        String[] flagTokens = stringFlags.split(",");
        for (String flagToken : flagTokens) {
            if ("OVERRIDE".equals(flagToken)) {
                flags |= OBOE_SETTINGS_FLAG_OVERRIDE;
            } else if ("SAMPLE_START".equals(flagToken)) {
                flags |= OBOE_SETTINGS_FLAG_SAMPLE_START;
            } else if ("SAMPLE_THROUGH".equals(flagToken)) {
                flags |= OBOE_SETTINGS_FLAG_SAMPLE_THROUGH;
            } else if ("SAMPLE_THROUGH_ALWAYS".equals(flagToken)) {
                flags |= OBOE_SETTINGS_FLAG_SAMPLE_THROUGH_ALWAYS;
            } else if ("TRIGGER_TRACE".equals(flagToken)) { 
                flags |= OBOE_SETTINGS_FLAG_TRIGGER_TRACE_ENABLED;
            } else if ("SAMPLE_BUCKET_ENABLED".equals(flagToken)) { //not used anymore
                flags |= OBOE_SETTINGS_FLAG_SAMPLE_BUCKET_ENABLED;
            } else {
                logger.debug("Unknown flag found from settings: " + flagToken);
            }
        }
        return flags;
    }
    
    @Override
    public long getValue() {
        return value;
    }

    @Override
    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public short getType() {
        return type;
    }

    @Override
    public short getFlags() {
        return flags;
    }
    
    @Override
    public String getLayer() {
        return layer;
    }
    
    @Override
    public long getTtl() {
        return ttl;
    }
    
    @Override
    public <T> T getArgValue(SettingsArg<T> arg) {
        return (T) args.get(arg);
    }
    
}