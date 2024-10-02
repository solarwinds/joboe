package com.tracelytics.test

import java.lang.reflect.Field
import java.lang.reflect.Method

import org.codehaus.groovy.grails.plugins.web.api.ControllersApi;

import com.tracelytics.test.Sample;

class HelloController {
    static layout = 'main';
    Random r  = new Random();
    def index() { 
        Thread.sleep(200);
        def theSample = new Sample(value: r.nextInt());
        render (view: "index", model: [sample: theSample]);
    }
    
    def filter() {
        Thread.sleep(200);
        def theSample = new Sample(value: r.nextInt());
        render (view: "index", model: [sample: theSample]);
    }
}
