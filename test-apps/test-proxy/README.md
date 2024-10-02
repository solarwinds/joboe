Instructions for testing reverse proxy on any http server:

1. Install nginx 
2. Use the `nginx.conf` in this directory, edit the `http.server.server_name` to match the deployed server name if accessed from non local-host location
3. By default this listens to port 80 and proxy calls to port 8080, adjust the conf otherwise
4. `service nginx restart`
5. The application should now be accessible at http://host-name (port 80)
