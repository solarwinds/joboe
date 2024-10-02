A simple app to test on various jdbc driver.

Please take note that sql syntax/host/ports varies driver to driver, the default values are for mysql, hence might need to be change to work on other drivers

To access it simply goto
http://localhost:8080/test-jdbc

Don't forget to setup the database before accessing it. Just google for downloads and installation steps

This app has only been tested and verified against:
MySQL (default username root) 
Postgres (default username postgres)
Derby (embedded)
Derby (service-client) (any non-empty username and password is ok)
Hsqldb (embedded) 
Hsqldb (client) (if used bin/runServer.bat to start the default db, use empty database name, username SA, empty password)

The rest of the drivers might require code adjustments to work properly


Click the initialize button to create a testing db and table before proceeding with querying. Leave the port field empty, and default port would be used 