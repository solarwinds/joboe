package com.appoptics.test;

import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicInteger;
import org.springframework.batch.item.*;

import java.util.Random;

public class TestItemReader extends AbstractItemReader {
    private static final int MAX_ITEMS = 100;
    private AtomicInteger count = new AtomicInteger(0);
    private Random random = new Random();

    public Object read() throws Exception, UnexpectedInputException, NoWorkFoundException, ParseException {
        if (count.incrementAndGet() <= MAX_ITEMS) {
            return new Integer(random.nextInt());
        } else {
            return null;
        }
    }


    public void reset() throws ResetFailedException {
        count.set(0);
    }
}
