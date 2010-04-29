/**
 * Copyright (c) Quicksilva, 2010
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the <organization> nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.qxlva.jira.services;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.ObjectUtils;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Period;
import org.joda.time.format.ISOPeriodFormat;

import com.atlassian.configurable.ObjectConfiguration;
import com.atlassian.configurable.ObjectConfigurationException;
import com.atlassian.core.user.UserUtils;
import com.atlassian.jira.ComponentManager;
import com.atlassian.jira.ManagerFactory;
import com.atlassian.jira.bc.JiraServiceContextImpl;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.event.issue.IssueEventDispatcher;
import com.atlassian.jira.event.type.EventType;
import com.atlassian.jira.event.type.EventTypeManager;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueFieldConstants;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.ModifiedValue;
import com.atlassian.jira.issue.priority.Priority;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.fields.config.manager.IssueTypeSchemeManager;
import com.atlassian.jira.issue.index.IssueIndexManager;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchResults;
import com.atlassian.jira.issue.util.DefaultIssueChangeHolder;
import com.atlassian.jira.jql.builder.JqlQueryBuilder;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.service.AbstractService;
import com.atlassian.jira.web.bean.PagerFilter;
import com.atlassian.query.Query;
import com.opensymphony.module.propertyset.PropertySet;
import com.opensymphony.user.EntityNotFoundException;
import com.opensymphony.user.User;

/**
 * @author Adrian Pillinger
 * 
 */
public class SLAServiceJob extends AbstractService {

	private static final Logger log = Logger.getLogger(SLAServiceJob.class);
	
	private static final String VERY_LARGE_SLA = "P10Y";
	private static final String OK = "OK";
	private static final String WARN = "WARN";
	private static final String BREACHED = "BREACHED";
	private static final String INCIDENT = "Incident";
	private static final String RESPONSE_SLA_BREACHED = "Response SLA Breached";
	private static final String _50_RESPONSE_SLA_TIME_ELAPSED = "50% Response SLA Time Elapsed";
	private static final String FIX_KPI_BREACHED = "Fix KPI Breached";
	private static final String _50_FIX_KPI_TIME_ELAPSED = "50% Response Fix KPI Time Elapsed";
	
	private static final String PRIORITY_1_INCIDENT_RAISED = "Priority 1 Incident Raised";
	public static final String PRIORITY_3 = "Priority 3";
	public static final String PRIORITY_2 = "Priority 2";
	public static final String PRIORITY_1 = "Priority 1";
	private static final String COMMA = ",";
	private static final String OP_HOURS_KEY_SUFFIX = "_OP_HOURS";
	private static final String FIX_KPIS_KEY_SUFFIX = "_FIX_KPIS";
	private static final String RESP_SLAS_KEY_SUFFIX = "_RESP_SLAS";
	private static final String PROJECTS_KEY_SUFFIX = "_PROJECTS";
	private static final String CLIENT_KEY_PREFIX = "CLIENT";
	private static final String SLA_SERVICE_USER = "SLA_SERVICE_USER";
	
	public static final String SECURITY_LEVEL = "Security Level";
	public static final String ON_HOLD = "On Hold";
//	public static final String INCIDENT_PRIORITY = "Incident Priority";
	public static final String TIME_ELAPSED = "Time Elapsed";
	public static final String FIX_KPI_STATE = "Fix KPI State";
	public static final String RESPONSE_SLA_STATE = "Response SLA State";
	public static final String SLA_LAST_CALCULATED = "SLA Last Calculated";
	public static final String DATE_RESPONDED = "Date Responded";
	public static final String DATE_RESOLVED = "Date Resolved";
	
	private static Set clientSLAConfigurations = new HashSet();
	private static String slaServiceUser = "admin"; // default to admin user
	public static Long P1_RAISED_EVENT = null;
	public static Long FIFTY_PERCENT_SLA_EVENT_ID = null;
	public static Long SLA_BREACHED_EVENT_ID = null;
	public static Long FIFTY_PERCENT_FIX_KPI_EVENT_ID = null;
	public static Long FIX_KPI_BREACHED_EVENT_ID = null;

	public SLAServiceJob() {

	}

	public void init(PropertySet props) throws ObjectConfigurationException {
		super.init(props);

		//**
//		log.debug(props.toString());
		//**
//		log.debug(props.getKeys().toString());
		if (hasProperty(SLA_SERVICE_USER)) {
			slaServiceUser = getProperty(SLA_SERVICE_USER);
		} else {
			log.warn("No service user specified for the SLA service. Using default instead: " + slaServiceUser);
		}

		String clientName;
		String projectsCsv;
		String[] projects;
		String responseSLACsv;
		String[] responseSLAs;
		String fixKPICsv;
		String[] fixKPIs;
		String operatingHours;
		int opHours;
		clientSLAConfigurations.clear();
		int i = 1;
		String clientKey = CLIENT_KEY_PREFIX + i;
		String clientProjectKey = clientKey + PROJECTS_KEY_SUFFIX;
		String clientResponseSLAKey = clientKey + RESP_SLAS_KEY_SUFFIX;
		String clientFixKPIKey = clientKey + FIX_KPIS_KEY_SUFFIX;
		String clientOperatingHoursKey = clientKey + OP_HOURS_KEY_SUFFIX;
		//**		
//		log.debug(clientKey + " : " + getProperty(clientKey));
		while (hasProperty(clientKey)) {

			clientName = null;
			projectsCsv = null;
			projects = null;
			responseSLACsv = null;
			responseSLAs = null;
			fixKPICsv = null;
			fixKPIs = null;
			operatingHours = null;
			opHours = -1;

			if (hasProperty(clientKey)) {
				clientName = getProperty(clientKey);
				//**		
//				log.debug(clientKey + " : " + getProperty(clientKey));

			}
			if (hasProperty(clientProjectKey)) {
				projectsCsv = getProperty(clientProjectKey);
				projects = projectsCsv.split(COMMA);
				//**		
//				log.debug(clientProjectKey + " : " + getProperty(clientProjectKey));
				//**		
//				log.debug(projects);
			}
			if (hasProperty(clientResponseSLAKey)) {
				responseSLACsv = getProperty(clientResponseSLAKey);
				responseSLAs = responseSLACsv.split(COMMA);
				//**		
//				log.debug(clientResponseSLAKey + " : " + getProperty(clientResponseSLAKey));
				//**		
//				log.debug(responseSLAs);
			}
			if (hasProperty(clientFixKPIKey)) {
				fixKPICsv = getProperty(clientFixKPIKey);
				fixKPIs = fixKPICsv.split(COMMA);
				//**		
//				log.debug(clientFixKPIKey + " : " + getProperty(clientFixKPIKey));
				//**		
//				log.debug(fixKPIs);
			}
			if (hasProperty(clientOperatingHoursKey)) {
				operatingHours = getProperty(clientOperatingHoursKey);
				if ("OFFICE_HOURS_9_TO_5_30".equals(operatingHours.trim())) {
					opHours = SLAWorkingHours.OFFICE_HOURS_9_TO_5_30;
				} else if ("TWENTYFOUR_SEVEN_365".equals(operatingHours.trim())) {
					opHours = SLAWorkingHours.TWENTYFOUR_SEVEN_365;
				}
				//**		
//				log.debug(clientOperatingHoursKey + " : " + getProperty(clientOperatingHoursKey));
				//**		
//				log.debug(""+opHours);
			}

			//**		
//			log.debug("About to create client config");
			final String responseSLA1 = responseSLAs == null ? null : responseSLAs[0];
			final String responseSLA2 = responseSLAs == null ? null : responseSLAs[1];
			final String responseSLA3 = responseSLAs == null ? null : responseSLAs[2];
			final String fixKPI1 = fixKPIs == null ? null : fixKPIs[0];
			final String fixKPI2 = fixKPIs == null ? null : fixKPIs[1];
			final String fixKPI3 = fixKPIs == null ? null : fixKPIs[2];
			ClientSLAConfig clientConfig = new ClientSLAConfig(clientName, projects, responseSLA1, responseSLA2, responseSLA3, fixKPI1, fixKPI2,
					fixKPI3, opHours);

			//**		
//			log.debug("About to add client config to configs, already at size: "+clientSLAConfigurations.size());
			clientSLAConfigurations.add(clientConfig);

			clientKey = CLIENT_KEY_PREFIX + ++i;
			clientProjectKey = clientKey + PROJECTS_KEY_SUFFIX;
			clientResponseSLAKey = clientKey + RESP_SLAS_KEY_SUFFIX;
			clientFixKPIKey = clientKey + FIX_KPIS_KEY_SUFFIX;
			clientOperatingHoursKey = clientKey + OP_HOURS_KEY_SUFFIX;
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.atlassian.jira.service.AbstractService#run()
	 */
	public void run() {
		log.warn("Running the SLA Service.");

		try {
			//**		
//			log.debug("Looking up user " + slaServiceUser);
			User user = UserUtils.getUser(slaServiceUser);
			if (user == null) {
				log.error("Cannot find user " + slaServiceUser);
			}

			// Get a search request service to use for searching
			final ComponentManager componentManager = ComponentManager.getInstance();
			final SearchService searchService = componentManager.getSearchService();
			final ProjectManager projectManager = componentManager.getProjectManager();
			final IssueTypeSchemeManager issueTypeSchemeManager = componentManager.getIssueTypeSchemeManager();
			final EventTypeManager eventTypeManager = componentManager.getEventTypeManager();
			final CustomFieldManager customFieldManager = componentManager.getCustomFieldManager();

			initialiseEventTypeIds(eventTypeManager);

			// create service context to execute search with
			JiraServiceContextImpl jiraServiceContextImpl = new JiraServiceContextImpl(user);

			final CustomField securityLevel = customFieldManager.getCustomFieldObjectByName(SECURITY_LEVEL);
			final CustomField dateResolved = customFieldManager.getCustomFieldObjectByName(DATE_RESOLVED);				
			final CustomField dateResponded = customFieldManager.getCustomFieldObjectByName(DATE_RESPONDED);				
			final CustomField slaLastCalculated = customFieldManager.getCustomFieldObjectByName(SLA_LAST_CALCULATED);				
			final CustomField responseSlaState = customFieldManager.getCustomFieldObjectByName(RESPONSE_SLA_STATE);				
			final CustomField fixSlaState = customFieldManager.getCustomFieldObjectByName(FIX_KPI_STATE);				
			final CustomField timeElapsed = customFieldManager.getCustomFieldObjectByName(TIME_ELAPSED);				
			final CustomField onHold = customFieldManager.getCustomFieldObjectByName(ON_HOLD);				
//			final CustomField incidentPriority = customFieldManager.getCustomFieldObjectByName(INCIDENT_PRIORITY);


			final Iterator iterator = clientSLAConfigurations.iterator();
			while (iterator.hasNext()) {
				final ClientSLAConfig clientSLAConfig = (ClientSLAConfig) iterator.next();
				//**
//				log.debug("processing slas for client: "+clientSLAConfig.getClientName());
				final String clientName = clientSLAConfig.getClientName();
				final String[] projects = clientSLAConfig.getProjects();
				Collection<Long> projectCollection = new HashSet<Long>();
				Collection<String> issueTypeCollection = new HashSet<String>();
				for (int i = 0; i < projects.length; i++) {
					final String projectName = projects[i] == null ? null : projects[i].trim();
					//**
//					log.debug("Found project name " + projectName);
					if (projectName != null) {
						final Project project = projectManager.getProjectObjByName(projectName);
						projectCollection.add(project.getId());
						final String id = getIncidentIssueId(issueTypeSchemeManager, project);
						if (id != null) {
							issueTypeCollection.add(id);
						}
					}
				}

				// skip client if there are no incident issue types
				if (issueTypeCollection.size() == 0) {
					log.warn("No projects for client: " + clientName + " have an issue type of Incident.");
					continue;
				}

				// create the search for the client and projects
				Query query = buildSearch(securityLevel, clientName, projectCollection, issueTypeCollection);
				//**
//				log.debug("Built search request.");

				try {
					SearchResults searchResults = searchService.search(jiraServiceContextImpl.getUser(), query, PagerFilter.getUnlimitedFilter());
					//**
//					log.debug("Searching for issues. Found ");

					List issues = searchResults.getIssues();
					//**
//					log.debug("Found " + issues.size());

					for (int i = 0; i < issues.size(); i++) {
						Issue issue = (Issue) issues.get(i);
						//**
//						log.debug("Updating SLA fields for issue " + issue.getKey());
						updateIssueSLAFields(clientSLAConfig, dateResolved, dateResponded, slaLastCalculated, responseSlaState, fixSlaState, timeElapsed,
								onHold, issue, user);

					}
				} catch (SearchException e) {
					log.error(e.getLocalizedMessage(), e);
				}
			}

		} catch (EntityNotFoundException e) {
			log.error(e.getLocalizedMessage(), e);
		}
	}

	/**
	 * Initialised the ids of the events we will fire
	 * 
	 * @param eventTypeManager
	 */
	private void initialiseEventTypeIds(final EventTypeManager eventTypeManager) {
		final Collection eventTypes = eventTypeManager.getEventTypes();
		final Iterator eventTypeIterator = eventTypes.iterator();
		while (eventTypeIterator.hasNext()) {
			EventType eventType = (EventType) eventTypeIterator.next();
			final String eventName = eventType.getName();
			if (PRIORITY_1_INCIDENT_RAISED.equals(eventName)) {
				P1_RAISED_EVENT = eventType.getId();
			} else if (_50_RESPONSE_SLA_TIME_ELAPSED.equals(eventName)) {
				FIFTY_PERCENT_SLA_EVENT_ID = eventType.getId();
			} else if (RESPONSE_SLA_BREACHED.equals(eventName)) {
				SLA_BREACHED_EVENT_ID = eventType.getId();
			} else if (FIX_KPI_BREACHED.equals(eventName)) {
				FIX_KPI_BREACHED_EVENT_ID = eventType.getId();
			} else if (_50_FIX_KPI_TIME_ELAPSED.equals(eventName)) {
				FIFTY_PERCENT_FIX_KPI_EVENT_ID = eventType.getId();
			}
		}
		if (P1_RAISED_EVENT == null) {
			log.warn("Cannot find event named \"" + PRIORITY_1_INCIDENT_RAISED + "\"");
		}
		if (FIFTY_PERCENT_SLA_EVENT_ID == null) {
			log.warn("Cannot find event named \"" + _50_RESPONSE_SLA_TIME_ELAPSED + "\"");
		}
		if (SLA_BREACHED_EVENT_ID == null) {
			log.warn("Cannot find event named \"" + RESPONSE_SLA_BREACHED + "\"");
		}
		if (FIX_KPI_BREACHED_EVENT_ID == null) {
			log.warn("Cannot find event named \"" + FIX_KPI_BREACHED + "\"");
		}
		if (FIFTY_PERCENT_FIX_KPI_EVENT_ID == null) {
			log.warn("Cannot find event named \"" + _50_FIX_KPI_TIME_ELAPSED + "\"");
		}
	}

	/**
	 * @param securityLevel
	 * @param clientName
	 * @param projectCollection
	 * @param issueTypeCollection
	 * @return
	 */
	private Query buildSearch(final CustomField securityLevel, final String clientName, Collection<Long> projectCollection, Collection<String> issueTypeCollection) {
		// create search for incidents
		JqlQueryBuilder newBuilder = JqlQueryBuilder.newBuilder();
		Query query = newBuilder.where()
			.project().inNumbers(projectCollection)
			.and().status().in("" + IssueFieldConstants.OPEN_STATUS_ID, 
					"" + IssueFieldConstants.INPROGRESS_STATUS_ID)
			.and().issueType().inStrings(issueTypeCollection)
			.and().customField(securityLevel.getIdAsLong()).like(clientName).buildQuery();
		return query;
	}

	/**
	 * @param issueTypeSchemeManager
	 * @param project
	 */
	private String getIncidentIssueId(final IssueTypeSchemeManager issueTypeSchemeManager, final Project project) {
		//**
		log.debug("Looking up Incident issue type");

		final Collection<IssueType> issueTypesForProject = issueTypeSchemeManager.getIssueTypesForProject(project);
		for (IssueType issueType : issueTypesForProject)
		{
			final String issueTypeName = issueType.getName();
			if (issueTypeName.equals(INCIDENT)) {
				final String id = issueType.getId();
				//**
//				log.debug("Found incident issue type with id: "+id);
				return id;
			}
		}
		log.warn("No Incident Issue Type Found");

		return null;
	}

	/**
	 */
	public static void updateIssueSLAFields(final ClientSLAConfig slaConfig, final CustomField dateResolved, final CustomField dateResponded,
			final CustomField slaLastCalculated, final CustomField responseSlaState, final CustomField fixKPIState, final CustomField timeElapsed,
			final CustomField onHold, Issue issue, User user) {
		final String key = issue.getKey();

		final Timestamp dateResolvedVal = (Timestamp) issue.getCustomFieldValue(dateResolved);
		final Timestamp dateRespondedVal = (Timestamp) issue.getCustomFieldValue(dateResponded);
		final Timestamp slaLastCalculatedVal = (Timestamp) issue.getCustomFieldValue(slaLastCalculated);
		final String responseSlaStateVal = (String) issue.getCustomFieldValue(responseSlaState);
		final String fixKPIStateVal = (String) issue.getCustomFieldValue(fixKPIState);
		final Period timeElapsedVal = (Period) issue.getCustomFieldValue(timeElapsed);
		final Timestamp onHoldVal = (Timestamp) issue.getCustomFieldValue(onHold);
        Priority priority = issue.getPriorityObject();
        final String incidentPriorityVal = priority.getName();

		long responseTime = getResponseSLAInMillis(slaConfig, incidentPriorityVal);
		long fixTime = getFixKPIInMillis(slaConfig, incidentPriorityVal);


		// if not on hold.
		if (onHoldVal == null) {
			//**
//			log.debug("Not on hold");
			// when it was last calculated
			Timestamp lastCalculatedTime = (Timestamp) slaLastCalculatedVal;
			// now
			Timestamp calculationTime = new Timestamp(System.currentTimeMillis());
			//**
			log.debug("lastCalculatedTime "+lastCalculatedTime);
			//**
//			log.debug("calculationTime "+calculationTime);

			if (lastCalculatedTime == null) {
				lastCalculatedTime = issue.getCreated();
			}
			//**
//			log.debug("lastCalculatedTime "+lastCalculatedTime);


			final Duration timeElapsedDuration = timeElapsedVal == null ? null : timeElapsedVal.toDurationTo(new DateTime());
			long upTilNow = timeElapsedDuration == null ? 0 : timeElapsedDuration.getMillis();
			//**
//			log.warn("Elapsed time so far" + upTilNow);

			//**
//			log.debug("upTilNow "+upTilNow);

			final Duration elapsedTime = calculateElapsedTime(lastCalculatedTime, calculationTime, slaConfig.getSlaOperatingHours());
			//**
//			log.warn("Elapsed time since last calculation" + upTilNow);

			//**
//			log.debug("elapsedTime "+upTilNow);

			long elapsedTimeInMillis = upTilNow + elapsedTime.getMillis();
			//**
//			log.warn("Elapsed time since last calculation + prev elapsed time" + elapsedTimeInMillis);

			if (slaLastCalculated.hasValue(issue)) {
				slaLastCalculated.updateValue(null, issue, new ModifiedValue(slaLastCalculatedVal, calculationTime), new DefaultIssueChangeHolder());
				//**
//				log.warn("Updating last calculated time" + calculationTime);

			} else {
				slaLastCalculated.createValue(issue, calculationTime);
			}
			//**
//			log.debug("Updated sla last calculated time");

			final Duration duration = new Duration(elapsedTimeInMillis);
			final Period period = duration.toPeriodTo(new DateTime());
			if (timeElapsed.hasValue(issue)) {
				//**
//				log.warn("Updating total elapsed time to" + period);
				timeElapsed.updateValue(null, issue, new ModifiedValue(timeElapsedVal, period), new DefaultIssueChangeHolder());
			} else {
				timeElapsed.createValue(issue, period);
			}
			//**
//			log.debug("Updated elapsed time");

			ComponentManager componentManager = ComponentManager.getInstance();
			final IssueManager issueManager = componentManager.getIssueManager();
			final CustomFieldManager customFieldManager = componentManager.getCustomFieldManager();

			// response SLA handling
			if (dateRespondedVal == null) {
				// breached
				if (elapsedTimeInMillis > responseTime) {
					if (!ObjectUtils.equals(BREACHED, responseSlaStateVal))
					{
						responseSlaState.updateValue(null, issue, new ModifiedValue(responseSlaStateVal, BREACHED), new DefaultIssueChangeHolder());
						log.debug("Dispatching breached SLA event.");
						dispatchEvent(issue, user, issueManager, customFieldManager, SLA_BREACHED_EVENT_ID);
					}
					
				}
				// warn at 50% response SLA elapsed
				else if (elapsedTimeInMillis > responseTime / 2)
				{
					if (!ObjectUtils.equals(WARN, responseSlaStateVal))
					{
						responseSlaState.updateValue(null, issue, new ModifiedValue(responseSlaStateVal, WARN), new DefaultIssueChangeHolder());
						log.debug("Dispatching 50% of SLA time elapsed.");
						dispatchEvent(issue, user, issueManager, customFieldManager, FIFTY_PERCENT_SLA_EVENT_ID);
					}
				}
				// SLA OK
				else if (!ObjectUtils.equals(OK, responseSlaStateVal)) {
					responseSlaState.updateValue(null, issue, new ModifiedValue(responseSlaStateVal, OK), new DefaultIssueChangeHolder());
				}
			}
			// resolved KPI handling
			if (dateResolvedVal == null) {
				if (elapsedTimeInMillis > fixTime) {
					if (!ObjectUtils.equals(BREACHED, fixKPIStateVal))
					{
						fixKPIState.updateValue(null, issue, new ModifiedValue(fixKPIStateVal, BREACHED), new DefaultIssueChangeHolder());
						log.debug("Dispatching breached fix KPI event.");
						dispatchEvent(issue, user, issueManager, customFieldManager, FIX_KPI_BREACHED_EVENT_ID);
					}
				} else if (elapsedTimeInMillis > fixTime / 2) {
					if (!ObjectUtils.equals(WARN, fixKPIStateVal))
					{
						fixKPIState.updateValue(null, issue, new ModifiedValue(fixKPIStateVal, WARN), new DefaultIssueChangeHolder());
						log.debug("Dispatching 50% of SLA time elapsed.");
						dispatchEvent(issue, user, issueManager, customFieldManager, FIFTY_PERCENT_FIX_KPI_EVENT_ID);
					}
				} else if (!ObjectUtils.equals(OK, fixKPIStateVal)) {
					fixKPIState.updateValue(null, issue, new ModifiedValue(responseSlaStateVal, OK), new DefaultIssueChangeHolder());
				}
			}
			//**
			log.debug("Updated response sla and fix kpi states");

			try {
				// reindex issue
				IssueIndexManager issueIndexManager = ManagerFactory.getIndexManager();
				issueIndexManager.reIndex(issue);
				log.debug("Reindexing issue");
			} catch (Exception e) {
				log.error("Failed to reindex issue: " + key, e);
			}
		}
	}

	/**
	 * @param issue
	 * @param user
	 * @param issueManager
	 */
	public static void dispatchEvent(Issue issue, User user, final IssueManager issueManager, final CustomFieldManager customFieldManager, Long eventId) {
        Priority priority = issue.getPriorityObject();
        final String incidentPriorityVal = priority.getName();
		
		final boolean isP1 = incidentPriorityVal.startsWith(PRIORITY_1);
		final boolean isP2 = incidentPriorityVal.startsWith(PRIORITY_2);
		final boolean isP3 = incidentPriorityVal.startsWith(PRIORITY_3);
		// NOTE Following 3 lines of comment are not applicable now, but left here in case we want to revert to this functionality. 
		// dispatch breach events for P2 and P3 breached SLAs
		// james doesn't want emails dispatched for P1 as he will
		// already be watching the issue like a hawk
		if (ObjectUtils.equals(eventId, SLA_BREACHED_EVENT_ID) && (isP1 || isP2 || isP3)) {
			//**
//			log.warn("Dispatching event: "+eventId);
			IssueEventDispatcher.dispatchEvent(eventId, issueManager.getIssueObject(issue.getId()), user);
		}
		// NOTE Following 3 lines of comment are not applicable now, but left here in case we want to revert to this functionality. 
		// dispatch warn events for P2 and P3 breached SLAs
		// james doesn't want emails dispatched for P1 as he will
		// already be watching the issue like a hawk
		else if (ObjectUtils.equals(eventId, FIFTY_PERCENT_SLA_EVENT_ID) && (isP1 || isP2 || isP3)) {
			//**
//			log.warn("Dispatching event: "+eventId);
			IssueEventDispatcher.dispatchEvent(eventId, issueManager.getIssueObject(issue.getId()), user);
		}
		// NOTE Following 3 lines of comment are not applicable now, but left here in case we want to revert to this functionality. 
		// dispatch breach events for P2 and P3 breached SLAs
		// james doesn't want emails dispatched for P1 as he will
		// already be watching the issue like a hawk
		if (ObjectUtils.equals(eventId, FIX_KPI_BREACHED_EVENT_ID) && (isP1 || isP2 || isP3)) {
			//**
//			log.warn("Dispatching event: "+eventId);
			IssueEventDispatcher.dispatchEvent(eventId, issueManager.getIssueObject(issue.getId()), user);
		}
		// NOTE Following 3 lines of comment are not applicable now, but left here in case we want to revert to this functionality. 
		// dispatch warn events for P2 and P3 breached SLAs
		// james doesn't want emails dispatched for P1 as he will
		// already be watching the issue like a hawk
		else if (ObjectUtils.equals(eventId, FIFTY_PERCENT_FIX_KPI_EVENT_ID) && (isP1 || isP2 || isP3)) {
			//**
//			log.warn("Dispatching event: "+eventId);
			IssueEventDispatcher.dispatchEvent(eventId, issueManager.getIssueObject(issue.getId()), user);
		}
		// dispatch warn events for P1 issues raised
		else if (ObjectUtils.equals(eventId, P1_RAISED_EVENT) && isP1) {
			//**
//			log.warn("Dispatching event: "+eventId);
			IssueEventDispatcher.dispatchEvent(eventId, issueManager.getIssueObject(issue.getId()), user);
		} else {
			log.warn("Not dispatching the event for eventId: " + eventId + ", and issue key: " + issue.getKey());
		}
	}

	/**
	 * Calculate the fix kpi for the priority of the issue
	 * 
	 * @param clientSLAConfig
	 * @param slaPriorityVal
	 * @return
	 */
	private static long getFixKPIInMillis(ClientSLAConfig clientSLAConfig, String slaPriorityVal) {
		long slaInMillis;
		String fixKPI = VERY_LARGE_SLA; // large sla that won't be broken as
										// default
		if (slaPriorityVal.startsWith(PRIORITY_1)) {
			fixKPI = clientSLAConfig.getP1FixKPI();
		} else if (slaPriorityVal.startsWith(PRIORITY_2)) {
			fixKPI = clientSLAConfig.getP2FixKPI();
		} else if (slaPriorityVal.startsWith(PRIORITY_3)) {
			fixKPI = clientSLAConfig.getP3FixKPI();
		}
		if (fixKPI == null)
		{
			log.debug("No value for the fix kpi has been specified for client " + clientSLAConfig.getClientName() + " not applying a KPI to the client.");
			fixKPI = VERY_LARGE_SLA;
		}
		final Duration duration = getDurationForISOPeriod(fixKPI);
		slaInMillis = duration.getMillis();	
		return slaInMillis;
	}

	/**
	 * Calculate the response sla for the priority of the issue
	 * 
	 * @param clientSLAConfig
	 * @param slaPriorityVal
	 * @return
	 */
	private static long getResponseSLAInMillis(ClientSLAConfig clientSLAConfig, String slaPriorityVal) {
		long slaInMillis;
		String responseSLA = VERY_LARGE_SLA; // 10 years SLA
		if (slaPriorityVal.startsWith(PRIORITY_1)) {
			responseSLA = clientSLAConfig.getP1ResponseSLA();
		} else if (slaPriorityVal.startsWith(PRIORITY_2)) {
			responseSLA = clientSLAConfig.getP2ResponseSLA();
		} else if (slaPriorityVal.startsWith(PRIORITY_3)) {
			responseSLA = clientSLAConfig.getP3ResponseSLA();
		}
		if (responseSLA == null)
		{
			log.debug("No value for the response sla has been specified for client " + clientSLAConfig.getClientName() + " not applying an SLA to the client.");
			responseSLA = VERY_LARGE_SLA;
		}
		final Duration duration = getDurationForISOPeriod(responseSLA);
		slaInMillis = duration.getMillis();	
	
		return slaInMillis;
	}

	/**
	 * Create the duration object for the ISO period string
	 * 
	 * @param periodString
	 * @return
	 */
	private static Duration getDurationForISOPeriod(final String periodString) {
		final Period period = ISOPeriodFormat.standard().parsePeriod(periodString);
		final Duration duration = period.toDurationFrom(new DateTime());
		return duration;
	}

	/**
	 * Calculate the elapsed time from the last calculated time up until the
	 * calculation time within the operating hours
	 * 
	 * @param lastCalculatedTime
	 * @param calculationTime
	 * @param slaOperatingHours
	 * @return
	 */
	private static Duration calculateElapsedTime(Timestamp lastCalculatedTime, Timestamp calculationTime, int slaOperatingHours) {
		SLATimeCalculator calculator = new SLATimeCalculator();
		final Duration elapsedTime = calculator.calculateElapsedTime(lastCalculatedTime, calculationTime, slaOperatingHours);
		return elapsedTime;
	}

	/**
	 * Get an unmodifiable set of client SLA configurations
	 * 
	 * @return
	 */
	public static Set getClientSLAConfigurations() {
		return Collections.unmodifiableSet(clientSLAConfigurations);
	}

	/**
	 * Get a client sla configuration for a given client and project
	 * 
	 * @return
	 */
	public static ClientSLAConfig getClientSLAConfiguration(String clientName, String projectName) {
		ClientSLAConfig clientConfig = null;
		if (clientName == null || clientName.trim().length() == 0 || projectName == null || projectName.trim().length() == 0) {
			return clientConfig;
		}
		final String clientNameUp = clientName.toUpperCase();
		final String projectNameUp = projectName.toUpperCase();

		final Iterator iterator = clientSLAConfigurations.iterator();
		while (iterator.hasNext()) {
			ClientSLAConfig curClientConfig = (ClientSLAConfig) iterator.next();
			final String clientCfgName = curClientConfig.getClientName();
			final String clientCfgNameUp = clientCfgName.toUpperCase();
			if (clientNameUp.equals(clientCfgNameUp)) {
				if (configContainsProject(projectNameUp, curClientConfig)) {
					clientConfig = curClientConfig;
					break;
				}
			}
		}
		return clientConfig;
	}

	/**
	 * Does the client sla configuration contain the project name provided?
	 * @param projectNameUp
	 *            Uppercase project name
	 * @param clientConfig
	 *            The client SLA configuration to look inside
	 */
	private static boolean configContainsProject(final String projectNameUp, ClientSLAConfig clientConfig) {
		final String[] projects = clientConfig.getProjects();
		for (int i = 0; i < projects.length; i++) {
			final String project = projects[i];
			final String projectUp = project.toUpperCase();
			if (projectNameUp.equals(projectUp)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Get the name of the user used by the sla service.
	 * @return
	 */
	public static String getSLAServiceUserName() {
		return slaServiceUser;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.atlassian.configurable.ObjectConfigurable#getObjectConfiguration()
	 */
	public ObjectConfiguration getObjectConfiguration() throws ObjectConfigurationException {
		final ObjectConfiguration objectConfiguration = getObjectConfiguration("slaservice", "com/qxlva/jira/services/sla/slaservice.xml", null);
		return objectConfiguration;
	}

	/* (non-Javadoc)
	 * @see com.atlassian.jira.service.AbstractService#destroy()
	 */
	public void destroy() {
		super.destroy();
		// make sure we don't keep the client config and add to it on the next startup
		clientSLAConfigurations.clear(); //
	}

}
