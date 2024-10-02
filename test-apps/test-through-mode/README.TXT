Simple testing app for traces that span across multiple hosts. This webapp is accessible by default at http://<host>/test-through-mode/  

for example:
http://localhost:8080/test-through-mode/

By entering a target URI, this webapp would issue a GET request to it. If the host of the target URI is also instrumented then there should be a trace that spans across both servers.

This can be used to verify the THROUGH mode is working properly on a remote server

