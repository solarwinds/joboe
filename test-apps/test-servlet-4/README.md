An app that tests server push in HTTP/2 in servlet 4.0

Take note that there are several pre-requisites:
1. The app server has to be servlet 4.0 compatible. Please use tomcat 9. Do NOT use tomcat 10 as it's servlet 5.0 which has different package name for servlets
2. Server push assumes TLS. As from wiki:
```
HTTP/2 is defined both for HTTP URIs (i.e. without encryption) and for HTTPS URIs (over TLS using ALPN extension[34] where TLS 1.2 or newer is required).

Although the standard itself does not require usage of encryption,[35] all major client implementations (Firefox,[36] Chrome, Safari, Opera, IE, Edge) have stated that they will only support HTTP/2 over TLS, which makes encryption de facto mandatory.[37]
``` Therefore the tomcat has to be configured to use TLS AND HTTP/2
3. Edit the server config, for tomcat, it's config/server.xml. Add section as below
```
<Connector
           protocol="org.apache.coyote.http11.Http11NioProtocol"
           port="8443" maxThreads="200"
           scheme="https" secure="true" SSLEnabled="true"
           keystoreFile="${user.home}/.keystore" keystorePass="changeit"
           clientAuth="false" sslProtocol="TLS">
		   <UpgradeProtocol className="org.apache.coyote.http2.Http2Protocol"/>
   </Connector>
``` For exact location, just search for `Http11NioProtocol` and you will likely find it commented out in the server.xml
4. Take note that the above config assume the keystore is at ${user/home}.keystore with password `changeit`. Using command below would by default add the key to ``.keystore`, use pw `changeit` when prompted:
```
Windows:

"%JAVA_HOME%\bin\keytool" -genkey -alias tomcat -keyalg RSA
Unix:

$JAVA_HOME/bin/keytool -genkey -alias tomcat -keyalg RSA
```
5. The app is available at https://localhost:8443/test-servlet-4/ . Take note that if you run from IDE (for example IntelliJ, you might need to set the HTTPS port here to 8443) https://intellij-support.jetbrains.com/hc/en-us/community/posts/360006489639-How-to-run-IntelliJ-tomcat-server-on-https-
6. If you inspect the network flow using browser, you should see that `lanlan.gif` has initiator `Push / test`
