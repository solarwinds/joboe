## Description

The following packages were downloaded and moved to the com.tracelytics.ext package namespace.

 Javassist 3.24.1-GA: https://github.com/jboss-javassist/javassist (renamed done automatically using shade plugin)
 
 ebson : https://github.com/kohanyirobert/ebson.git c5ba053bcd619ba947a70f00a52fffb007d37b4e  Sat Mar 10 17:12:19 2012 +0100

 Google Guava Libraries 11.0.2 (a dependency of ebson) : http://code.google.com/p/guava-libraries/ v11.0.2 tag

 JSR-305 (a dependency of Guava) : http://code.google.com/p/jsr-305/  direct from trunk, there are no tags or versions.

 Apache httpcore 4.4.1 : https://github.com/apache/httpcore
 
 Apache httpclient 4.4.1 : https://github.com/apache/httpclient
 
 Apache thrift 0.9.3 : https://github.com/apache/thrift
 
The renaming is done in case the customer code we are instrumenting happens to use different
versions of these packages. We don't want to cause a version conflict.

Renaming is as follows: 
   javassist => com.tracelytics.ext.javassist
   ebson     => com.tracelytics.ext.ebson
   guava     => com.tracelytics.ext.google
   jsr305    => com.tracelytics.ext.javax.annotation

## Compile/Build
#### Thrift
If there's any change to the thrift prototype in `collector.thrift`, then we would need to re-generate the thrift code

1. Download thrift executable [v0.93](http://archive.apache.org/dist/thrift/0.9.3/). Please use this version as it matches the Maven dependency.
2. Make sure in `collector.thrift`, the java namespace is `namespace java com.appoptics.ext.thriftgenerated`
3. path/to/thrift -r --gen java <project directory>/src/main/thrift/collector.thrift
4. code will be generated to the `gen-java` folder of the current location
5. copy all the files under `gen-java\com\appoptics\ext\thriftgenerated` to the corresponding package of this project
 

#### General 
Since some of these packages do not run on JDK 11, we force the maven to use JDK 8 to compile. 

If your maven is run using JDK 8, then the `mvn clean install` should run as is.

If your maven is run using JDK 9+, then you would need to either set environment variable `JAVA_1_8_HOME` to your JDK 8 installation directory or replace in the pom.xml the `JAVA_1_8_HOME` property value to the same installation.
