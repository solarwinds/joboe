###  Tracemode Test App for Java

A test app that sends a get request to a page in `localhost:8080`.

Sample usage: Say the test-app is deployed on an appserver using port 8081, a call to `http://localhost:8081/test-tracemode/?mode=always` will redirect to `http://localhost:8080/always.html` and return its contents. `http://localhost:8081/test-tracemode/` will redirect to `http://localhost:8080/index.html`.

NOTE: This is intended to be used by [Java Tests set up in Docker](https://github.com/tracelytics/oboe-test/wiki/Doing-Testing-with-Docker), the pages this app redirects to will be spun up as part of the Docker image build ([The Java Stack](https://github.com/tracelytics/oboe-test/tree/docker/java/java-stack), [an example of the java-stack being deployed during build](https://github.com/tracelytics/oboe-test/blob/docker/automation/docker/dockerfiles/srv2/jboss6_Dockerfile#L21-L25)).
