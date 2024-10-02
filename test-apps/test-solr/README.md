## For Solr 3/4

This project contains solr server binary(as war) as well as the data required to run Solr3 and Solr4/Solr4.10.3 (related to https://github.com/tracelytics/joboe/issues/345) server on tomcat

1. Copy the 3 wars from <this project>/solr-war to tomcat's webapp folder

2. Copy the 3 config XMLs from <this project>/solr-conf to tomcat's conf folder under $TOMCAT_HOME/conf/Catalina/localhost

3. Modify the 3 config XML such that the environment variable solr/home has value pointing at your solr data folder (the <this project>/solr-3-example and <this project>/solr-4-example)
solr-3.xml:
<Environment name="solr/home" type="java.lang.String" value="<YOUR PROJECT LOCATION HERE>/solr-3-example/solr" override="true"/>
solr-4.xml/solr-4.10.3.xml:
<Environment name="solr/home" type="java.lang.String" value="<YOUR PROJECT LOCATION HERE>/solr-4-example/solr" override="true"/>

4. Start your tomcat server, the Solr servers admin console are accessible via:
http://localhost:8080/solr-3 
http://localhost:8080/solr-4
http://localhost:8080/solr-4.10.3
** If there are problems of Filter start, reference here https://wiki.apache.org/solr/SolrLogging#Using_the_example_logging_setup_in_containers_other_than_Jetty **

## For Solr 5+

1. Download Solr from https://lucene.apache.org/solr/downloads.html and extract the downloaded file

2. Include the javaagent by modifying <solr dir>\bin\solr.in.cmd (windows) solr.in.sh (linux). add
For windows, there's some weird escape problem for `=` and `,`, it would require `^` preceding them to work properly, for example:
```
set SOLR_OPTS=%SOLR_OPTS% -javaagent:"C:/Users/patson.luk/git/joboe/core/target/appoptics-agent_proguard_base.jar^=config^=C:/Users/patson.luk/git/joboe/core/src/test/java/javaagent.json^,service_key^=ec3d1519afe2f54474d3d3cc2c9af0aff9f6e939c0d6302d768d808378025468:patson" 
```

3. Navigate to the extract directory and start example solr with `bin/solr start -e cloud` (for linux) or `bin\solr.cmd start -e cloud` (for windows)  

4. Use all the defaults (keep hitting enter)

5. The admin console is accessible via:
http://localhost:8983

 


## Test steps
1. From the admin console, you can test on querying as well as various admin function

2. To test data import, please use commands as below:
a. Insert data directly via Solr admin console (solr 4.4)
   -Select collection1 in the core dropdown
   -Select documents tab
   -Insert some data and submit 

b. Index binary documents such as Word, PDF, html with Solr Cell  http://wiki.apache.org/solr/ExtractingRequestHandler
   -Take note that the war for solr-4 has already been modified to include necessary extension library for the parsing
   -To index a document, use command as below. Replace the literal.id value with a unique ID and the myfile value with the document location
   For example:
   curl "http://localhost:8080/solr-4/update/extract?literal.id=doc2&commit=true" -F "myfile=@tutorial.pdf"
   curl "http://localhost:8080/solr-4/update/extract?literal.id=doc3&commit=true" -F "myfile=@/usr/share/apache-solr-3.1.0/docs/features.pdf"
   
   -If it shows exception of write.lock then make sure you have write/read permission to <YOUR PROJECT LOCATION HERE>/solr-4-example/solr and all sub-directories
   
c. Use the simple data import page by building and deploying this project
   -mvn clean package, copy the war file in target folder to your application server
   -The page is accessible via
   http://localhost:8080/test-solr   
   -Choose to insert data to solr 3 or solr 4 server
   
7. If you want to populate with MORE data
Introducing Sprawler!!! 
http://localhost:8080/test-solr/sprawler.do
Just enter a base URL and it will visit that URL, get the contents and insert to Solr, Solr would also extract the links, then the sprawler would recursively
look up contents of those links, until it reaches 100 results or n level of recursive calls (the other input parameter)

**For Solr 5+, ensure the solrconfig.xml has node `requestHandler` set as below (the fmap.a part)**
```
<requestHandler name="/update/extract"
                  startup="lazy"
                  class="solr.extraction.ExtractingRequestHandler" >
    <lst name="defaults">
      <str name="lowernames">true</str>
      <str name="uprefix">ignored_</str>

      <!-- capture link hrefs but ignore div attributes -->
      <str name="captureAttr">true</str>
      <str name="fmap.a">links</str>
      <str name="fmap.div">ignored_</str>
    </lst>
  </requestHandler>
```

**The start link should not be a redirect, make sure it returns http status 200**

The end result is ur Solr data set (only insert to solr-4 collection1 for now) will grow with data parsed from various web pages. 
Then you can search based on description, for example:
http://localvm:8080/solr-4/collection1/select?q=description%3Ajava&rows=10&wt=json&indent=true

**The operation will take time as it has to visit each URL (threadpool at 5), the screen will keep updating with the current progress**




