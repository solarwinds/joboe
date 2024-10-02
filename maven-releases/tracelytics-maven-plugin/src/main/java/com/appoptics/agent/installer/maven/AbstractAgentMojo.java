package com.appoptics.agent.installer.maven;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.Authentication;
import org.apache.maven.artifact.repository.MavenArtifactRepository;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.dependency.GetMojo;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.zip.ZipUnArchiver;
import org.codehaus.plexus.logging.console.ConsoleLogger;

/**
 * Parent class for the goals in the maven plugin that performs the installation of the Java agent.
 *
 * @author ibakalov
 */
@SuppressWarnings("deprecation")
public abstract class AbstractAgentMojo extends AbstractMojo  {
    
    private static final String MAVEN_SONATYPE_URL = "https://oss.sonatype.org/content/groups/public";
    private static final String MAVEN_CENTRAL_URL = "https://repo1.maven.org/maven2";
    protected static final String CONFIG_FILE_NAME = "javaagent.json";
    private static final String AGENT_PACKAGING = "zip";
    private static final String AGENT_ARTIFACT_ID = "appoptics-agent";
    private static final String AGENT_GROUP_ID = "com.appoptics.agent.java";
    private static final String ARTIFACT_DEFAULT_VERSION = "RELEASE";
    
    /**
     * Contains the injected value of the configuration property for the agent location. 
     */
    @Parameter( property = "agentLocation", defaultValue = "" )
    protected String agentLocation;

    /**
     * Contains the injected value of the configuration property for the agent version. 
     */
    @Parameter( property = "agentVersion", defaultValue = ARTIFACT_DEFAULT_VERSION )
    protected String agentVersion;

    @Component
    private ArtifactFactory artifactFactory;

    @Component
    private ArtifactResolver artifactResolver;

    /**
     * Maven build logger
     */
    protected SystemStreamLog log = new SystemStreamLog();

    /**
     * represents the project model 
     */
    private MavenProject mavenProject = null;
    
    /**
     *  The method that is called by Maven when the plugin goal is executed.
     * @see org.apache.maven.plugin.Mojo#execute()
     */
    public void execute() throws MojoExecutionException, MojoFailureException {
        
        mavenProject = (MavenProject)(getPluginContext().get("project"));
        
        List<OSType> targetOSTypes = getTargetOSTypes();
        
        String targetLocation = getTargetUnzipLocation(); 
        
        log.info("Installing/Unzipping agent in location: " + targetLocation);

        String temporaryZipFileName = targetLocation + File.separatorChar + "appoptics-agent.zip";
        
        getArtifact(mavenProject, AGENT_GROUP_ID, AGENT_ARTIFACT_ID, agentVersion, AGENT_PACKAGING, temporaryZipFileName); 
        
        beforeUnzip(targetLocation);
        
        File temporaryZipFile = new File(temporaryZipFileName);
        try {
            unzip(temporaryZipFile, new File(targetLocation));
        } catch (Exception e) {
            MojoFailureException mojoFailureException = new MojoFailureException("Error while unzipping agent: " + e.getMessage(), e);
            throw mojoFailureException;
        }
        
        afterUnzip(targetLocation);
        
        if (temporaryZipFile.exists()) {
           temporaryZipFile.delete();
        }
    }
    
    /**
     * Called to perform actions before the actual unzipping, i.e. preparing backups 
     * @param targetLocation the target location where the unzipping is performed
     */
    abstract protected void afterUnzip(String targetLocation);

    /**
     * Called to perform actions after the actual unzipping, i.e. restoring backups 
     * @param targetLocation the target location where the unzipping is performed
     */
    abstract protected void beforeUnzip(String targetLocation);

    /**
     * Determines the target operating systems and native library files required.
     *   
     * @return The target (non local) operating system(s) for agents. If "all" is defined, it might return a set of systems.
     * @throws MojoFailureException 
     */
    abstract protected List<OSType> getTargetOSTypes() throws MojoFailureException;

    /**
     * Determines the target location for the agent files unzip.
     * @return the target location
     * @throws MojoFailureException 
     */
    abstract protected String getTargetUnzipLocation() throws MojoFailureException;

    /**
     * Unzips a specified zip file to a specified folder. Uses plexus archiver.
     * @param zipFile the zip file to unzip.
     * @param targetDir the folder to unzip the zip file to.
     * @throws Exception if an error occurs while unzipping.
     */
    private void unzip(File zipFile, File targetDir) throws Exception {
        targetDir.mkdirs();
        ZipUnArchiver unzipper = new ZipUnArchiver(zipFile); 
        unzipper.enableLogging(new ConsoleLogger(ConsoleLogger.LEVEL_DISABLED, "zip_logger"));
        unzipper.setOverwrite(true);
        unzipper.setDestDirectory(targetDir);
        unzipper.extract();
    }
    

    /**
     * Acquires a specified maven artifact. Invokes code from the Maven dependency plugin to achieve this.
     * 
     * @param mavenProject project model (the information from the current pom where the plugin is invoked from) 
     * @param groupId the group id of the artifact to be acquired
     * @param artifactId the id of the artifact to be acquired  
     * @param version the version of the artifact to be acquired
     * @param packaging the packaging of the artifact to be acquired
     * @param targetFile the file where the artifact needs to be written to
     * @throws MojoExecutionException if an error occurs during the dependency injection or the execution of the dependency:get code
     * @throws MojoFailureException if an error occurs during the execution of the dependency:get code
     */
    private void getArtifact(MavenProject mavenProject, String groupId, String artifactId, String version, String packaging, String targetFile) throws MojoExecutionException, MojoFailureException {
        
        GetMojo getMojo = new GetMojo();
        getMojo.setPluginContext(getPluginContext());
        
        List<ArtifactRepository> repositories = getRepositories(mavenProject); 

        inject(getMojo, "pomRemoteRepositories", repositories);
        inject(getMojo, "transitive", Boolean.FALSE);
        inject(getMojo, "destination", targetFile);
        inject(getMojo, "artifact", groupId + ':' + artifactId + ':' + version);
        inject(getMojo, "packaging", packaging);
        
        inject(getMojo, "artifactFactory", artifactFactory);
        inject(getMojo, "artifactResolver", artifactResolver);
        
        getMojo.execute();
    }

    /**
     * Prepares a list of repositories to look up for the artifact into. If a maven project is available then it will retrieve the repositories from it.
     * Otherwise it will include maven central repository, the public sonatype repository and any repository specified via system properties.
     * @param mavenProject the maven project to get the repositories form
     * @return a list of repositories.
     */
    private List<ArtifactRepository> getRepositories(MavenProject mavenProject) {

        List<ArtifactRepository> repositories = null; 

        DefaultRepositoryLayout layout = new DefaultRepositoryLayout();
        ArtifactRepositoryPolicy releasePolicy = new ArtifactRepositoryPolicy(true, ArtifactRepositoryPolicy.UPDATE_POLICY_ALWAYS, ArtifactRepositoryPolicy.CHECKSUM_POLICY_FAIL);
        ArtifactRepositoryPolicy snapshotPolicy = new ArtifactRepositoryPolicy(true, ArtifactRepositoryPolicy.UPDATE_POLICY_ALWAYS, ArtifactRepositoryPolicy.CHECKSUM_POLICY_FAIL);
        if (mavenProject == null || mavenProject.getRemoteArtifactRepositories() == null) {
            repositories = new ArrayList<ArtifactRepository>();
            MavenArtifactRepository central = new MavenArtifactRepository("central", MAVEN_CENTRAL_URL, layout, snapshotPolicy, releasePolicy);
            MavenArtifactRepository sonatype = new MavenArtifactRepository("sonatype", MAVEN_SONATYPE_URL, layout, snapshotPolicy, releasePolicy);
            repositories.add(sonatype);
            repositories.add(central);
        } else {
            repositories = mavenProject.getRemoteArtifactRepositories();  
        }
        String userRepositoryURL = System.getProperty("repositoryURL");
        String userRepositoryUser = System.getProperty("repositoryUser");
        String userRepositoryPass = System.getProperty("repositoryPass");
        if (userRepositoryURL != null) {
            MavenArtifactRepository userRepository = new MavenArtifactRepository("userRepository", userRepositoryURL, layout, snapshotPolicy, releasePolicy);
            if (userRepositoryUser != null) {
                userRepository. setAuthentication(new Authentication(userRepositoryUser, userRepositoryPass));
            }
            repositories.add(userRepository);
        }
        return repositories;
    }

    
    /**
     * Injects a dependency in the specified object (the dependency injection is commonly used in Maven).
     * @param o the object to inject into
     * @param fieldName the field to be injected into
     * @param value the value to be injected
     * @throws MojoExecutionException if there is error while injecting the value.
     */
    private void inject(Object o, String fieldName, Object value) throws MojoExecutionException {

        @SuppressWarnings("rawtypes")
        Class clazz = o.getClass();
        Field afield;
        try {
            afield = clazz.getDeclaredField(fieldName);
            afield.setAccessible(true);
            afield.set(o, value);
        } catch (Exception e) {
            throw new MojoExecutionException("Could not inject a value in the dependencies plugin: " + e.getMessage());
        }
    }
    
    /**
     * @return true if the plugin is called from command line.
     */
    protected boolean isCommandLine() {
        return "standalone-pom".equals(mavenProject.getArtifactId()) &&
               "org.apache.maven".equals(mavenProject.getGroupId()) &&
               mavenProject.getName() != null &&
               mavenProject.getName().indexOf("(No POM)") != -1;
    }
    
    protected enum OSType {
        WINDOWS, LINUX, UNKNOWN;
    }
}
