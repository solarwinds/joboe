The app is accessible by default at http://<host>/test-xmemcached/ (max supported version 2.0.0)

for example:
http://localhost:8080/test-xmemcached/

The page takes in a key, duration and value, and would perform all of the instrumented method calls (which are available to this version) within the handling.

For the actual method calls, please refer to the class TestAll

To setup the memcached server, run apt-get install memcached