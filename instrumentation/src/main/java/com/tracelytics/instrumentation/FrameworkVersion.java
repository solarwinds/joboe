package com.tracelytics.instrumentation;

public class FrameworkVersion {

    private final int majorVersion;
    private final int minorVersion;

    public FrameworkVersion(int majorVersion, int minorVersion) {
        this.majorVersion = majorVersion;
        this.minorVersion = minorVersion;
    }

    public boolean isNewerOrEqual(FrameworkVersion compareToVersion) {
        if (this.majorVersion > compareToVersion.majorVersion) {
            return true;
        } else if (this.majorVersion < compareToVersion.majorVersion) {
            return false;
        } else {
            return this.minorVersion >= compareToVersion.minorVersion;
        }
    }

    public boolean isOlderOrEqual(FrameworkVersion compareToVersion) {
        if (this.majorVersion > compareToVersion.majorVersion) {
            return false;
        } else if (this.majorVersion < compareToVersion.majorVersion) {
            return true;
        } else {
            return this.minorVersion <= compareToVersion.minorVersion;
        }
    }

    public int getMajorVersion() {
        return majorVersion;
    }

    public int getMinorVersion() {
        return minorVersion;
    }
}
