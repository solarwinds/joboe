Test case for using OT API compliant SDK (either OT SDK or our AO SDK - which is a wrapper of OT SDK) along with our javaagent

Example usage:
- Starting a trace using OT sdk, and have AO agent auto instrumentation to add child spans to it (for example a batch job with JDBC operations)
- A spring boot application (trace started by our agent) with custom OT spans created in controller 