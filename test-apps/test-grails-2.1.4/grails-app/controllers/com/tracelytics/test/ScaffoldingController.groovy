package com.tracelytics.test

import java.lang.reflect.Field
import java.lang.reflect.Method

import org.codehaus.groovy.grails.plugins.web.api.ControllersApi;

import com.tracelytics.test.Sample;

class ScaffoldingController {
    static layout = 'main';
    static scaffold = Sample;
}
