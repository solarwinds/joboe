#!/bin/sh

java -javaagent:../core/target/tracelyticsagent.jar \
     -cp target/testapp-1.0-jar-with-dependencies.jar \
     com.tracelytics.testapp.TestSettingsReader $*
