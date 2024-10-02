package com.appoptics.test;

import javax.batch.api.chunk.AbstractItemWriter;
import java.util.ArrayList;
import java.util.List;

public class ExceptionChunkWriter extends AbstractItemWriter {
    List<Integer> processed = new ArrayList<>();
    @Override
    public void writeItems(List<Object> items) throws Exception {
        items.stream().map(Integer.class::cast).forEach(processed::add);
        throw new RuntimeException("Test exception");
    }
}