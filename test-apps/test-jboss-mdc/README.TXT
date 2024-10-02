Test jboss MDC, modify the domain configuration file (for location see points below) . Do NOT modify the logging.properties as such a file gets overwritten on sub-system loading

```
<formatter name="PATTERN">
    <pattern-formatter pattern="AO %X{ao.traceId} %d{yyyy-MM-dd HH:mm:ss,SSS} %-5p [%c] (%t) %s%e%n"/>
</formatter>

<formatter name="COLOR-PATTERN">
    <pattern-formatter pattern="AO %X{ao.traceId} %K{level}%d{HH:mm:ss,SSS} %-5p [%c] (%t) %s%e%n"/>
</formatter>
```

Since the actual library used depends on the jboss/wildfly runtime, we can use this same application to test different jboss MDC versions:

- deploy to the latest wildfly application server for the latest MDC, config file location: <wildfly installation>\standalone\configuration\standalone.xml
- deploy to jboss 6.0.0 (jboss-logmanager 1.2.0.CR9) for earliest MDC support https://github.com/jboss-logging/jboss-logmanager/blob/1.1/src/main/java/org/jboss/logmanager/MDC.java . Config file location: <jboss installation>\server\default\deploy\jboss-logging.xml
- jboss 5.1 uses jboss-logmanager 1.0 which delegates to log4j MDC so it works (no need to test)
- jboss 5.0 and eariler does not use jboss-logmanager and delegates to log4j MDC so it works (no need to test)