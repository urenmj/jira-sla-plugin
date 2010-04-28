To build deploy the plugin to a test JIRA instance you need to 

1. Download the Atlassian Plugin SDK from http://confluence.atlassian.com/display/DEVNET/How+to+Build+an+Atlassian+Plugin
   At the time of writing this was version 3.0.6
2. Unpack the SDK into C:\Atlassian
3. Add the SDK bin directory to your path. For example add C:\Atlassian\atlassian-plugin-sdk-3.0.6\bin to your path.
4. At the command prompt within the plugin's project directory execute "atlas-run" to compile the plugin and run a test instance of JIRA at http://localhost:1990/jira/


Deploying the plugin...
1. Stop JIRA
2. Install the jar file into plugins/installed-plugins/ 
3. IN JIRA 4 THE PLUGIN ARCHITECTURE CHANGED. SO THIS STEP NEEDS CHECKING AS OF JIRA VERSION 4 to see what libs are required, or if they need to be deployed to a new location. Make sure the plugins in the plugins folder are put into the WEB-INF/lib directory as these are requiured for some of the service delivery jira instance configuration. 
4. Restart JIRA.

To configure JIRA for the SLA service see SLA Instructions.txt

Remote debugging can be performed on port 5005

A default has a default account of admin/admin.
