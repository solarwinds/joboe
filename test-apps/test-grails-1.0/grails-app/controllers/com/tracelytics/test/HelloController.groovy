package com.tracelytics.test

class HelloController {
	Random r  = new Random();

    def index = {
        Thread.sleep(200); 
        def theSample = new Sample(value: r.nextInt());
        render (view: "index", model: [sample: theSample]); 
    }
    
    
    def filter = {
        Thread.sleep(200);
        def theSample = new Sample(value: r.nextInt());
        render (view: "index", model: [sample: theSample]);
    }
}
