### Description ###
This documents our Maven delivery of our API jars, agent (as a zip) and agent installation (as maven plugin)

- We want to support the same use cases (both SDK and agent) as New relic and if possible, provide more or better options.
- We want to provide AppOptics java SDK as a project dependency.
- We want to provide AppOptics java agent jar as a download available from Maven central repository
- We want to be able to release our maven artifacts easily, preferably to the Maven central repository.


### Solution ###
2 Maven artifacts are to be published:
- appoptics-sdk - this is the public name for the "SDK" artifact, which is being released as a jar file. No separate build is needed to build the public SDK. User can include this in their maven project directly as a dependency (3rd party library)
- appoptics-agent - a jar file, containing the agent only


#### Maven artifacts ####
##### New project struture #####
The structure of our "joboe" project (and build) is as follows:
```
Appoptics-Parent
      |
      +---- Core            
      |
      +---- Agent (published as artifact appoptics-agent)
      |
      +---- Instrumentation            
      |         
      +---- Dependencies        
      |                 
      +---- SDK (published as artifact appoptics-sdk)        
      |
      +---- maven-releases
                |
                +---- tracelytics-api-deployer
                |                                                            
                +---- tracelytics-agent-zip (legacy naming, it only deploys a jar right now)    
                |                                                            
                +---- tracelytics-maven-plugin (not in used)
```

##### appoptics-parent #####
This refers to the existing "joboe" folder, which contains core, api and dependencies. And we will add a new sub-project `maven-releases`
* We added a pom.xml in the parent directory, which is a common parent that defines the following properties:
 * `<agent.version>` - defines the agent version, which will be copied to the `versions.properties` file during the build. Agent code logic in runtime will now read the version number from that properties file instead of the constant field
 * `<api.version>` - defines the agent api version, currently the same as `agent.version`
 * `<maven.plugin.version>` - our maven agent plugin version, this reflects the versioning of our plugin installer code which is independent to the agent.
 * `<is.snapshot>` - specifies if the versions are releases or snapshots. By default they are releases, but the value of this property is overridden in a maven profile called "snapshot" that can be called from command line with "-P snapshot".

##### Existing sub-projects #####
* Core, Dependencies and API, keep them in their current state, these artifacts are only for internal use. They should not and will not be put directly into a public repository.

##### New maven-releases sub-project #####
* maven-releases: Reactor POM for building and deploying all of the public artifacts. Contains all of the maven related code as well. The pom defines maven specific properties:
 * `<repository.url>` - specifies the repository url to deploy the maven artifacts to. Would be different for staging and snapshots. The pom files contains both the staging and snapshot URLs, the URL used depending on the profile used. By default it uses the staging URL
 * `<serverId>` - specifies the server that the artifacts will be deployed to (staging or snapshot). The ID corresponds to different target server which require login credentials with is NOT included in the pom itself. The maven settings file must have credentials specified for the servers. These credentials can be found in the [secret server as "sonatype.org"](https://secretserver.solarwinds.com/SecretServer/SecretView.aspx?secretid=3699).
* appoptics-agent - If build target `deploy` is defined, then it will as well gpg sign and deploy the `appoptics-agent` artifact (jar) to the staging OSSRH repository.
* tracelytics-api-deployer - contains scripts that gpg sign and deploy the `appoptics-sdk` artifact to the staging OSSRH repository
* tracelytics-agent-zip (**unused**) - contains the script for building of the zip file, containing the public api, the agent and the configuration file. If build target `deploy` is defined, then it will as well gpg sign and deploy the `appoptics-agent` artifact to the staging OSSRH repository.
* tracelytics-maven-plugin (**unused**) - contains code that installs agent into the system according to various params (os/version). If build target `deploy` is defined, then it will as well gpg sign and deploy the `tracelytics-maven-plugin` artifact to the staging OSSRH repository

#### Release Process ####
##### Deployment machine setups 
Please refer to https://github.com/librato/trace/blob/master/docs/java/setup/build-machine.md#signing-and-publishing-of-maven-artifacts

##### General release to Maven Central (release) #####
- From project `maven-releases`, run `mvn clean deploy`. This builds dependencies, core and api in correct order and deploy the built artifacts to the corresponding Sonatype **Staging** repo. This gpg signs the artifact with the sign key installed in the build machine which is described in previous section, the password is stored in AWS and imported to maven before passing into the `mvn` command line. Besides the default profile (that builds **ONLY** the api artifact), there are also 3 other profiles `tracelytics-agent-zip`, `tracelytics-api-deployer` and `tracelytics-maven-plugin` that only deploy individual artifact. 
- The artifacts is only accessible with staging credentials found in https://secretserver.solarwinds.com/SecretServer/SecretView.aspx?secretid=3699
- If all tests pass, then we first need to "close" the staging repositories by going to the Sonatype UI https://oss.sonatype.org/ with credentials mentioned above. The closing of the repo takes a few mins with some validations
- If all the validations passed, then we can "promote" the closed staging repositories to "release" by using the same Sonatype UI. We also need to notify the Sonatype team on the first release at https://issues.sonatype.org/browse/OSSRH-34417
- Can check https://status.maven.org/ for Sonatype system status

##### General release to Maven Central (snapshot) #####
Similar to the above but once published, it's accessible to the public.
