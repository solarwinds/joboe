#!/bin/sh

java -javaagent:../core/target/tracelyticsagent.jar=sampling_rate=1000000,debug=true \
     -cp target/testapp-1.0-jar-with-dependencies.jar \
     com.tracelytics.testapp.TestContextAPI
