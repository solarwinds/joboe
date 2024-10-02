package com.tracelytics.test

class TestFilters {
    static filters = {

        filterRequest(controller:"*", action:"filter") {
            before = {
                Thread.sleep(100);
            }
            
            after = {
                Thread.sleep(100);
            }
            
            afterView = {
                Thread.sleep(100);
            }
            
        }


    }
}
