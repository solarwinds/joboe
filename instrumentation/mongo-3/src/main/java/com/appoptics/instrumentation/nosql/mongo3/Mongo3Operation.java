package com.appoptics.instrumentation.nosql.mongo3;


public interface Mongo3Operation {
    String tvGetCollectionName();
    String tvGetDatabaseName();
    String tvGetOperationType();
}
