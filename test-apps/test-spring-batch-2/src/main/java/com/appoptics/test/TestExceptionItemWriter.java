package com.appoptics.test;

import org.springframework.batch.item.support.AbstractItemStreamItemWriter;

import java.util.List;

public class TestExceptionItemWriter extends AbstractItemStreamItemWriter {
    @Override
    public void write(List list) throws Exception {
        throw new RuntimeException("Testing exception");
    }
}
