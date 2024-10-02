llib=/usr/local/lib
java -cp $llib/libthrift-0.8.0.jar:$llib/slf4j-api-1.5.8.jar:$llib/slf4j-log4j12-1.5.8.jar:$llib/log4j-1.2.14.jar:tserver.jar client.ClientNoInst $*
