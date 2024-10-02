The app is accessible by default at http://<host>/test-xmemcached-min/ (min supported version 2.10.0)

for example:
http://localhost:8080/test-xmemcached-min/

The page takes in a key, duration and value, and would perform all of the instrumented method calls (which are available to this version) within the handling.

For the actual method calls, please refer to the class TestAll

To setup the memcached server, run apt-get install memcached
