package com.tracelytics.instrumentation.http.play;

import com.tracelytics.ext.javassist.CtClass;
import com.tracelytics.ext.javassist.NotFoundException;
import com.tracelytics.instrumentation.ClassInstrumentation;

/**
 * Abstract base class for Play instrumentation
 * @author Patson Luk
 *
 */
public abstract class PlayBaseInstrumentation extends ClassInstrumentation {
    protected static final String LAYER_NAME = "play";
    protected enum Version {
        PLAY_2_0(2,0),
        PLAY_2_1(2,1),
        PLAY_2_2(2,2),
        PLAY_2_3(2,3),
        PLAY_2_4(2,4),
        PLAY_2_5(2,5),
        PLAY_2_6(2,6);

        private final int majorVersion;
        private final int minorVersion;

        Version(int majorVersion, int minorVersion) {
            this.majorVersion = majorVersion;
            this.minorVersion = minorVersion;
        }

        public boolean isNewerOrEqual(Version compareToVersion) {
            if (this.majorVersion > compareToVersion.majorVersion) {
                return true;
            } else if (this.majorVersion < compareToVersion.majorVersion) {
                return false;
            } else {
                return this.minorVersion >= compareToVersion.minorVersion;
            }
        }

        public boolean isOlderOrEqual(Version compareToVersion) {
            if (this.majorVersion > compareToVersion.majorVersion) {
                return false;
            } else if (this.majorVersion < compareToVersion.majorVersion) {
                return true;
            } else {
                return this.minorVersion <= compareToVersion.minorVersion;
            }
        }

    }

    private static ThreadLocal<Integer> depthThreadLocal = new ThreadLocal<Integer>() {
        protected Integer initialValue() {
            return 0;
        }
    };


    /**
     * Checks whether the current instrumentation should start a new extent. If there is already an active extent, then do not start one
     * @return
     */
    protected static boolean shouldStartExtent() {
        int currentDepth = depthThreadLocal.get();
        depthThreadLocal.set(currentDepth + 1);

        if (currentDepth == 0) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Checks whether the current instrumentation should end the current extent. If this is the active extent being traced, then ends it
     * @return
     */
    protected static boolean shouldEndExtent() {
        int currentDepth = depthThreadLocal.get();
        depthThreadLocal.set(currentDepth - 1);

        if (currentDepth == 1) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Returns whether there are any active extent
     * @return
     */
    static boolean hasActiveExtent() {
        return depthThreadLocal.get() != null && depthThreadLocal.get() > 0;
    }

   /**
    * Gets Play major version 1 or 2
    * @return
    */
    protected int getPlayMajorVersion() {
        try {
            classPool.get("play.mvc.Result"); //only available in 2.0 +
            return 2;
        } catch (NotFoundException e) {
            logger.debug("Cannot load play.mvc.Result, probably running Play 1");
            return 1;
        }
    }

    /**
     * Convenient method to identify Play scala version
     * @param cc
     * @return
     */
    protected Version identifyVersion() {
        try {
            CtClass actionBuilderClass= classPool.get("play.api.mvc.ActionBuilder"); //only 2.1 + has ActionBuilder
            try {
                actionBuilderClass.getDeclaredMethod("async"); //only 2.2+ has async method in the ActionBuilder
                try {
                    classPool.get("play.api.mvc.RequestHeader").getDeclaredMethod("attrs"); //only 2.6+ has RequestHeader.attrs()
                    return Version.PLAY_2_6;
                } catch (NotFoundException e) {
                    return Version.PLAY_2_2;
                }
            } catch (NotFoundException e) { 
                return Version.PLAY_2_1;
            }
        } catch (NotFoundException e) {
            return Version.PLAY_2_0;
        }
    }
}
