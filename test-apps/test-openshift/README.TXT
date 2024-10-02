1. Openshift provides a complete application environment with strict control. Our java agent is installed to the webapp via their "cartridge" system
2. First open an account on https://www.openshift.com/
3. Install client tool for command line access https://www.openshift.com/developers/rhc-client-tools-install
4. In command line, create a testing jboss as 7 app via (or any other cartridge type rhc app create to see a full list of available cartridges) : 
    rhc app create testos jbossas-7 (replace `testos` with any app name you want)
5. Take note of the URL, SSH and Git Remote location of your app from step 4. You will need them later    
6. Access the web app by the URL appeared on the screen from step 4. Verify it's working
6. Now to add traceylzer and javaagent to the app:
 -set the enviroment variable
    rhc set-env TRACEVIEW_ACCESS_KEY=Your_Access_Key -a testos
 -get the cartridge 
    rhc cartridge-add https://raw.githubusercontent.com/appneta/openshift-cartridge-traceview/master/metadata/manifest.yml -a testos
7. Now make the jboss to use our javaagent, take note that Openshift does not allow changing standalone.conf directly. We have to use
a pre-action hook in order to get the variable set before Jboss startup. In order to push the hook, you would need to first clone the project
to your local machine: (install git if you haven't)
    git clone <git_url> <directory to create>
8. navigate to the directory just create, then go into .openshift/action_hooks, create a new file with name "pre_start_jbossas-7" (replace jbossas-7 with cartridge name of other stack):
    vim pre_start_jbossas-7
9. in the file insert line below and save it:
    export _JAVA_OPTIONS="-javaagent:$OPENSHIFT_TRACEVIEW_DIR/tracelyticsagent.jar"
10. push the change to the server:
    git add .
    git commit -m "added pre start action hook"
    git push
    
11. JBoss will be restarted automatically, access the URL again and check for traces
    
    
Optional:    
- To follow the log from local machine:
    rhc tail testos
- Setup putty for ssh client access, for key generation, follow: https://www.openshift.com/developers/remote-access

Remarks:
rhc app restart does NOT work as it does not trigger the pre_start hook properly. The problem has been reported on http://stackoverflow.com/questions/24788862/openshift-pre-start-hook-does-not-work-on-app-restart
    
       