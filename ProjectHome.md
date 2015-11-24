# Introduction #

The SLA Plugin provides a simple implementation of SLAs in JIRA to allow it to be used in a service delivery/help desk environment. Initially it is not intended to work for everyones situation, but to be used as a start to customise your own setup.


# JIRA Plugin Development #

Atlassian provide comprehensive [documentation](http://confluence.atlassian.com/display/DEVNET/How+to+Build+an+Atlassian+Plugin) for plugin development on their [developer network](http://confluence.atlassian.com/display/DEVNET/How+to+Build+an+Atlassian+Plugin).

This plugin is a version 2 plugin with respect to Atlassian's plugin architecture. This means that the plugin should be deployed to plugins/installed-plugins/ directory or JIRA 4 and onwards.

# Quick start to the plugin #

To build deploy the plugin to a test JIRA instance you need to

1. Download the Atlassian Plugin SDK from http://confluence.atlassian.com/display/DEVNET/How+to+Build+an+Atlassian+Plugin
At the time of writing this was version 3.0.6. This is also attached to this page in case it becomes unavailable.
2. Unpack the SDK into C:\Atlassian
3. Add the SDK bin directory to your path. For example add C:\Atlassian\atlassian-plugin-sdk-3.0.6\bin to your path.
4. At the command prompt within the plugin's project directory execute "atlas-run" to compile the plugin and run a test instance of JIRA at http://localhost:1990/jira/


Deploying the plugin...
1. Stop JIRA
2. Install the jar file into plugins/installed-plugins/
3. Restart JIRA.

Remote debugging can be performed on port 5005

A default has a default account of admin/admin.

# SLA Service Installation #

1. Install the plugin jar file into plugins/installed-plugins/ and restart JIRA.

2. Create the following priorities with a description, icon, and colour

Priority 1 - Not Usable
Priority 2 - Severe Limitation
Priority 3 - Slight Limitation

3. Create any new Statuses for issues, for example On Hold

4. Create a workflow and on the relevant transitions add post functions. e.g.

On transition Create Issue add post function "SLA Issue Created"
On transition Start Work add post function "Responded to client"
On transition Put On Hold add post function "Issue placed on hold"
On transition Restart work add post function "Issue taken off hold"
On transition Close add post function "Resolved the issue"

5. Create the following events from Administration \-> Global Settings \-> Events

Name: "Response SLA Breached"
Name: "50% Response SLA Time Elapsed"
Name: "Fix KPI Breached"
Name: "50% Response Fix KPI Time Elapsed"
Name: "Priority 1 Incident Raised"

Provide all an appropriate description and select Temple: "Generic Event"

6. Create a new notification scheme if required (this step may be skipped if you want to amend an existing notification scheme)

7. Edit notifications for the new events in your notification scheme. For example send notifications to the Project Lead or a Service Delivery Manager role.

The events to modify notifications are:
Response SLA Breached
50% Response SLA Time Elapsed
Fix KPI Breached
50% Response Fix KPI Time Elapsed
Priority 1 Incident Raised

8. Create new issue type
Incident

9. Create new Issue Type scheme if required
e.g. Service Delivery Issue Type Scheme

10. Add the Incident issue type to the Issue Type Scheme

11. Associate the Issue Type Scheme with the required projects

12. Create a new Workflow Scheme if required

13. Add a workflow to the scheme. Select the Incident issue type and the Incident work flow you created in step 2.

14. Assign the workflow scheme created or used in 10 to the appropriate projects

15. Add custom fields

Type: Date Time
Name: "Date Resolved"
Search Template: Date Time Range Picker
Issue Types: Incident
Context: Global
Display on Default Screen

Type: Date Time
Name: "Date Responded"
Search Template: Date Time Range Picker
Issue Types: Incident
Context: Global
Display on Default Screen

Type: Date Time
Name: "SLA Last Calculated"
Search Template: None
Issue Types: Incident
Context: Global
Display on None

Type: Select List
Name: "Response SLA State"
Search Template: Multi Select Searcher
Issue Types: Incident
Context: Global
Display on Default Screen

Type: Select List
Name: "Fix KPI State"
Search Template: None
Issue Types: Incident
Context: Global
Display on None

Type: Duration Custom Field
Name: "Time Elapsed"
Search Template: None
Issue Types: Incident
Context: Global
Display on Default Screen

Type: Date Time
Name: "On Hold"
Search Template: Date Time Range Picker
Issue Types: Incident
Context: Global
Display on Default Screen

Type: Security Level Searcher
Name: "Security Level"
Search Template: Text Searcher
Issue Types: Any Issue
Context: Global
Display on None

Type: Select List
Name: "Incident Priority"
Search Template: Multi Select Searcher
Issue Types: Incident
Context: Global
Display on Default Screen

16. Configure custom fields "Response SLA State" and "Fix KPI State" with values: OK, WARN, BREACHED. Default OK

17. Create a user for the SLA service with full permissions. e.g. slaservice

18. Re Index JIRA

19. Add new service: Administration \-> System \-> Services
Name: SLA Service
Class: com.qxlva.jira.services.SLAServiceJob
Delay: 2

Configure the clients as required