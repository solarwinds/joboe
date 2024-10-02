The app is accessible by default at http://<host>/test-spymemcached-2-7-3/ (2.7.3)

for example:
http://localhost:8080/test-spymemcached-2-7-3/

The page takes in a key, duration and value, and would perform all of the instrumented method calls (which are available to this version) within the handling.

For the actual method calls, please refer to the class SetAction

To setup the memcached server, run apt-get install memcached

For multiple nodes testing, please refer to the class SetAction's execute method or class GetBulkMultipleNodes. Take note that the 2nd server address is hard-coded
and should be set to something else if such a server is not reachable/available from the deployed location.