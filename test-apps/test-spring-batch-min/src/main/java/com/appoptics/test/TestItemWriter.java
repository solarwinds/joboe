package com.appoptics.test;

import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicInteger;
import org.springframework.batch.item.*;

import java.util.Random;

public class TestItemWriter extends AbstractItemWriter {

    public void write(Object o) throws Exception {
        System.out.println("writing " + o);
    }
}
