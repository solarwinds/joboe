package com.tracelytics.util;

import java.util.ArrayList;
import java.util.List;

public class JavaVersionComparator {
    public static int compare(String v1, String v2) {
        return new Version(v1).compare(new Version(v2));
    }

    private static class Version {
        List<Integer> versions = new ArrayList<>();
        Version(String v) {
            String[] arr = v.split("[_.]");
            for (String ver : arr) {
                try {
                    versions.add(Integer.valueOf(ver));
                } catch (NumberFormatException e) {
                    versions.add(0);
                }
            }
        }

        int compare(Version other) {
            int selfIdx = 0, otherIdx = 0;
            while (selfIdx < versions.size() && otherIdx < other.versions.size()) {
                int selfVersion = versions.get(selfIdx);
                int otherVersion = other.versions.get(otherIdx);

                if (selfVersion != otherVersion) {
                    return selfVersion - otherVersion;
                }
                selfIdx++;
                otherIdx++;
            }
            if (selfIdx == versions.size() && otherIdx == other.versions.size()) return 0;
            return selfIdx == versions.size() ? -1 : 1;
        }
    }
}
