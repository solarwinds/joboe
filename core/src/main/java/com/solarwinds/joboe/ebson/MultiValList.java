package com.solarwinds.joboe.ebson;

import java.util.ArrayList;

/**
 * Hack that allows multiple key/value pairs with the same key to be added to a BSON document.
 * This list just contains the values. We look for it when generating the BSON in DefaultWriter.
 */
public class MultiValList<T> extends ArrayList<T> {

    public MultiValList() {
        super();
    }

    public MultiValList(int initialCapacity) {
        super(initialCapacity);
    }
}
