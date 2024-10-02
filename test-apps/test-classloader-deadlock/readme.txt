compile TestDriverMultipleClassLoader.java

then run it with/without javaagent:

java -javaagent:/home/pluk/git/joboe/core/target/tracelyticsagent_proguard_base.jar=config=/home/pluk/git/joboe/core/conf/javaagent.json test.TestDriverMultipleClassLoader

java test.TestDriverMultipleClassLoader


You will notice that with agent, it will not exit. Try to kill it with kill -3 <pid> and you will see the stacktrace very similar to https://github.com/tracelytics/joboe/issues/183
