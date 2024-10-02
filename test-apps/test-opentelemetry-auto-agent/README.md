For using AO Span Exporter along with OT auto agent

This includes a springboot application with NO code changes. 

All the instrumentation will be provided by the OT auto agent along with AO Span Exporter.

For example the JVM argument to start the Spring boot application (`TestSpringBootApplication`) would be
-javaagent:"C:\Users\Patson\Downloads\opentelemetry-auto-0.2.2.jar" -Dota.exporter.jar="C:\Users\Patson\git\joboe\open-telemetry-auto-agent-exporter\target\appoptics-telemetry-auto-agent-exporter-jar-with-dependencies.jar" -Dota.exporter.appoptics.service.key=ec3d1519afe2f54474d3d3cc2c9af0aff9f6e939c0d6302d768d808378025468:ot-auto-agent-batch









