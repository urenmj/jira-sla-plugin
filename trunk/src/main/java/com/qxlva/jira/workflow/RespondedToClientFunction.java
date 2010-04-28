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
package com.qxlva.jira.workflow;

import java.sql.Timestamp;
import java.util.Map;

import org.ofbiz.core.entity.GenericValue;

import com.atlassian.core.user.UserUtils;
import com.atlassian.jira.ComponentManager;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.ModifiedValue;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.util.DefaultIssueChangeHolder;
import com.atlassian.jira.project.Project;
import com.opensymphony.module.propertyset.PropertySet;
import com.opensymphony.user.EntityNotFoundException;
import com.opensymphony.user.User;
import com.opensymphony.workflow.FunctionProvider;
import com.opensymphony.workflow.WorkflowException;
import com.qxlva.jira.services.ClientSLAConfig;
import com.qxlva.jira.services.SLAServiceJob;

/**
 * @author Adrian Pillinger
 *
 */
public class RespondedToClientFunction implements FunctionProvider {

	/* (non-Javadoc)
	 * @see com.opensymphony.workflow.FunctionProvider#execute(java.util.Map, java.util.Map, com.opensymphony.module.propertyset.PropertySet)
	 */
	public void execute(Map transientVars, Map args, PropertySet ps)
			throws WorkflowException {
		
		MutableIssue mIssue = (MutableIssue) transientVars.get("issue");
		
		ComponentManager componentManager = ComponentManager.getInstance();
		final CustomFieldManager customFieldManager = componentManager.getCustomFieldManager();
		final CustomField dateResolved = customFieldManager.getCustomFieldObjectByName(SLAServiceJob.DATE_RESOLVED);				
		final CustomField dateResponded = customFieldManager.getCustomFieldObjectByName(SLAServiceJob.DATE_RESPONDED);				
		final CustomField slaLastCalculated = customFieldManager.getCustomFieldObjectByName(SLAServiceJob.SLA_LAST_CALCULATED);				
		final CustomField responseSlaState = customFieldManager.getCustomFieldObjectByName(SLAServiceJob.RESPONSE_SLA_STATE);				
		final CustomField fixSlaState = customFieldManager.getCustomFieldObjectByName(SLAServiceJob.FIX_KPI_STATE);				
		final CustomField timeElapsed = customFieldManager.getCustomFieldObjectByName(SLAServiceJob.TIME_ELAPSED);				
		final CustomField onHold = customFieldManager.getCustomFieldObjectByName(SLAServiceJob.ON_HOLD);				
		final CustomField incidentPriority = customFieldManager.getCustomFieldObjectByName(SLAServiceJob.INCIDENT_PRIORITY);				

		
		final Project project = mIssue.getProjectObject();
		final GenericValue securityLevel = mIssue.getSecurityLevel();
		
		final ClientSLAConfig clientSLAConfiguration = SLAServiceJob.getClientSLAConfiguration(securityLevel.getString("name"), project.getName());
		if (clientSLAConfiguration != null)
		{
			User user;
			try {
				user = UserUtils.getUser(SLAServiceJob.getSLAServiceUserName());
			} catch (EntityNotFoundException e) {
				e.printStackTrace();
				throw new WorkflowException(e.getMessage());
			}
			SLAServiceJob.updateIssueSLAFields(clientSLAConfiguration, dateResolved, dateResponded,
					slaLastCalculated, responseSlaState,
					fixSlaState, timeElapsed, onHold, incidentPriority, mIssue, user);
		}
		final Object dateRespondedVal = mIssue.getCustomFieldValue(dateResponded);
		dateResponded.updateValue(null, mIssue, new ModifiedValue(dateRespondedVal, new Timestamp(System.currentTimeMillis())), new DefaultIssueChangeHolder());

	}

}
