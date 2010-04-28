/*
 * Copyright (c) 2006, ILOG.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 *    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *    * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 *    * Neither the name of the <ORGANIZATION> nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.qxlva.jira.customfield;

import org.apache.log4j.Category;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Period;
import org.joda.time.format.ISOPeriodFormat;

import com.atlassian.jira.issue.customfields.impl.AbstractSingleFieldType;
import com.atlassian.jira.issue.customfields.impl.FieldValidationException;
import com.atlassian.jira.issue.customfields.manager.GenericConfigManager;
import com.atlassian.jira.issue.customfields.persistence.CustomFieldValuePersister;
import com.atlassian.jira.issue.customfields.persistence.PersistenceFieldType;

/**
 * Custom field for storing org.joda.time.Duration and represents the value as
 * an ISO8601 period format.
 */
public class DurationCFType extends AbstractSingleFieldType {

	public DurationCFType(
			CustomFieldValuePersister customFieldValuePersister,
			GenericConfigManager genericConfigManager) {
		super(customFieldValuePersister, genericConfigManager);
	}
	
	private static final Category log = Logger.getLogger(DurationCFType.class);

	protected PersistenceFieldType getDatabaseType() {
		return PersistenceFieldType.TYPE_DECIMAL;
	}

	protected Object getDbValueFromObject(Object customFieldObject) {
		if (customFieldObject == null)
		{
            return null;
		}
        assertObjectImplementsType(Period.class, customFieldObject);
        Period period = (Period) customFieldObject;
        final Duration durationFrom = period.toDurationFrom(new DateTime());
		final Double dbVal = new Double(durationFrom.getMillis());

		return dbVal;
	}

	protected Object getObjectFromDbValue(Object arg0)
			throws FieldValidationException {
        assertObjectImplementsType(Double.class, arg0);
        Double dbl = (Double) arg0;
        Period period = new Period(dbl.longValue());
		return period;
	}

	public Object getSingularObjectFromString(String arg0)
			throws FieldValidationException {
		final Period period = ISOPeriodFormat.standard().parsePeriod(arg0);
		return period;
	}

	public String getStringFromSingularObject(Object arg0) {
        assertObjectImplementsType(Period.class, arg0);
        Period period = (Period) arg0;
		final String isoPeriodString = period.toString();
		return isoPeriodString;
	}

}
