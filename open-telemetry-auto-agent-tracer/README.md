Failed attempt to export the `TraceProvider` as a service

The idea is to provide our own `TraceProvider` (`com.appoptics.api.ext.AppOpticsAutoAgentTraceProvider`) that instantiates a `TracerProviderSdk` with our own AppOptics sampler

This is required if we want to run the OpenTelemetry auto agent along with our own sampling logic.

Unfortunately, this does NOT work (at least with the attempt so far) as the service locator would look up the service definition file
for the auto-agent first, which points at the default `TracerProviderSdk` (instead of our `com.appoptics.api.ext.AppOpticsAutoAgentTraceProvider`)

Still committing this artifact for reference, or perhaps we could find some way to make this work in the future...