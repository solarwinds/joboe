package com.appoptics.test;

import org.springframework.batch.item.support.AbstractItemStreamItemWriter;

import java.util.List;

public class TestItemWriter extends AbstractItemStreamItemWriter {
    @Override
    public void write(List list) throws Exception {
        System.out.println("writing " + list);
    }
}
