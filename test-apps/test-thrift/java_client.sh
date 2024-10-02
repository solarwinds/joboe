llib=/usr/local/lib
java -javaagent:/usr/local/tracelytics/tracelyticsagent.jar=sampling_rate=1000000 -cp $llib/libthrift-0.8.0.jar:$llib/slf4j-api-1.5.8.jar:$llib/slf4j-log4j12-1.5.8.jar:$llib/log4j-1.2.14.jar:tserver.jar client.Client $*
