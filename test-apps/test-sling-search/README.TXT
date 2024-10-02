This creates a simple executable jar that will issue a SEARCH request against a Sling server in DavEx (WebDAV)

To build run:
mvn clean compile assembly:single

Then run the jar with:
java -jar test-sling-search-jar-with-dependencies.jar

By default it will query endpoint: 
    http://localhost:8080/server/default/jcr:root 
with query:
    SELECT * FROM [nt:base] WHERE [jcr:primaryType] = 'nt:unstructured'
of query language:
    JCR-SQL2
    

To use parameters other than those above. Either provide 2 arguments [query], [query language] or 3 arguments [query], [query language], [host]

for example java -jar test-sling-search-jar-with-dependencies.jar "//*[@jcr:primaryType='nt:unstructured']" "xpath"



To setup the Sling server:
Goto https://sling.apache.org/downloads.cgi

Download the Sling Standalone application or the Sling Web Application. Simply start the jar or deploy the war to application server of your choice. Don't forget to include the correct
javaagent arguments to enable our agent if it's in standalone mode.

As of date that this is written, Sling provides war for Sling-5 and Sling-6. The latest trunk is Sling-7, which a downloadable build was not available.

Therefore 2 extra builds are attached in the of "wars" folder on this project for testing convenience:
-sling-7.war based on current trunk (Sling JCR resource 2.3.5.SNAPSHOT, Sling api 2.6.1.SNAPSHOT, Sling engine 2.3.2 - git hash
-sling-ea.war based on EA environment (Sling JCR resource 2.2.2, Sling api 2.3.0, Sling Engine 2.2.6 - git hash 8e136e508ce4ac5c92036c637f83731bd78f337c)

sling-5.war and sling-6.war are also included. Take note that sling-5 is NOT supported

Or to build the jar/war of the Sling source, check out Sling at https://github.com/apache/sling:
-do NOT use the tags, as they are for each separate component and will not build the complete jar/war
-use the same JAVA_OPT version as mentioned in the readme. Using a different java version for maven might result in failed build
-some of the commits could be broken and does not build properly
-might be faster to skip test. use: "mvn clean package -Dmaven.test.skip=true"
-the built jar/war could be found in <sling home>\launchpad\builder\target

Since the above testing client points at the ROOT of the application server, the Sling war has to be deployed to the ROOT. (http://localhost:8080/)

Sling runs as OSGi, the console can be accessed via:
http://localhost:8080/system/console/bundles

#### ATTENTION: to switch from version to version, you might have to delete the "sling" and "jackrabbit" folders in the running folder! (app server start folder)


Extra Credit!
The Sling deployed above has homepage
http://localhost:8080
and Sling resource Explorer
http://localhost:8080/.explorer.html

which are both written in Sling. Therefore accessing those pages would trigger sling traces which demonstrate Sling non DavEx(WebDAV) access.
