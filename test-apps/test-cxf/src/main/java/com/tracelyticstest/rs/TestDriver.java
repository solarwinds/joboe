package com.tracelyticstest.rs;

import java.util.Collections;

import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.provider.json.JSONProvider;

public class TestDriver {
    public static void main(String[] args) {
        TracelyticsClient client = JAXRSClientFactory.create("https://api.tracelytics.com/api-v1", TracelyticsClient.class, Collections.singletonList(new JSONProvider()));
        Data data = client.getResult("Default", "9de3ed4c-5e33-4e26-b0b7-82c20ea01532");
                
        System.out.println(data.average);
        System.out.println(data.count);
        System.out.println(data.latest);
    }
}
