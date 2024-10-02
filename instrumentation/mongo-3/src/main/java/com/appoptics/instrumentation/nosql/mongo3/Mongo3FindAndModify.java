package com.appoptics.instrumentation.nosql.mongo3;

public interface Mongo3FindAndModify {
    Object tvGetModifyTarget();
    Object tvGetFilter();
    String tvGetQueryOp();
}
