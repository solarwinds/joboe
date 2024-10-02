package com.appoptics.test;

import org.springframework.batch.item.*;
import org.springframework.batch.item.support.AbstractItemStreamItemReader;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class TestItemReader extends AbstractItemStreamItemReader {
    private static final int MAX_ITEMS = 100;
    private AtomicInteger count = new AtomicInteger(0);
    private Random random = new Random();

    @Override
    public Object read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
        if (count.incrementAndGet() <= MAX_ITEMS) {
            return Integer.valueOf(random.nextInt());
        } else {
            return null;
        }
    }
//
//    public Object read() throws Exception, UnexpectedInputException, NoWorkFoundException, ParseException {
//        if (count.incrementAndGet() <= MAX_ITEMS) {
//            return new Integer(random.nextInt());
//        } else {
//            return null;
//        }
//    }
//
//
//    public void reset() throws ResetFailedException {
//        count.set(0);
//    }
}
