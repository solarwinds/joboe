#!/bin/sh

java -javaagent:../core/target/tracelyticsagent.jar=sampling_rate=1000000 \
     -cp target/testapp-1.0-jar-with-dependencies.jar \
     -Djava.library.path=/usr/local/tracelytics \
     com.tracelytics.testapp.TestNonWebApp
