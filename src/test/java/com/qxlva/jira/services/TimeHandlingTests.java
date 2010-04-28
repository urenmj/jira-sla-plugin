package com.qxlva.jira.services;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Period;
import org.joda.time.format.ISOPeriodFormat;

public class TimeHandlingTests extends TestCase {
	private static final SLATimeCalculator calculator = new SLATimeCalculator();


	public void testStartBeforeEndInWorkingHours() {
		DateTime lastCalculated = new DateTime(2009, 6, 22, 8, 55, 0, 0);
		DateTime now = new DateTime(2009, 6, 22, 12, 00, 0, 0);

		Duration elapsedTime = calculator.calculateElapsedTime(lastCalculated, now, SLAWorkingHours.OFFICE_HOURS_9_TO_5_30);
		Assert.assertEquals("PT3H", elapsedTime.toPeriod().toString());
	}

	public void testStartBeforeEndAfterWorkingHours() {
		DateTime lastCalculated = new DateTime(2009, 6, 22, 8, 55, 0, 0);
		DateTime now = new DateTime(2009, 6, 22, 18, 00, 0, 0);

		Duration elapsedTime = calculator.calculateElapsedTime(lastCalculated, now, SLAWorkingHours.OFFICE_HOURS_9_TO_5_30);
		Assert.assertEquals("PT8H30M", elapsedTime.toPeriod().toString());
	}

	public void testStartInEndInWorkingHours() {
		DateTime lastCalculated = new DateTime(2009, 6, 22, 14, 05, 0, 0);
		DateTime now = new DateTime(2009, 6, 22, 14, 17, 0, 0);

		Duration elapsedTime = calculator.calculateElapsedTime(lastCalculated, now, SLAWorkingHours.OFFICE_HOURS_9_TO_5_30);
		Assert.assertEquals("PT12M", elapsedTime.toPeriod().toString());
	}

	public void testSpanTwoDaysOfOfficHoursA() {
		DateTime lastCalculated = new DateTime(2009, 6, 22, 9, 5, 0, 0);
		DateTime now = new DateTime(2009, 6, 24, 9, 5, 0, 0);

		Duration elapsedTime = calculator.calculateElapsedTime(lastCalculated, now, SLAWorkingHours.OFFICE_HOURS_9_TO_5_30);
		Assert.assertEquals("PT17H", elapsedTime.toPeriod().toString());
	}

	public void testSpanTwoDaysOfOfficeHoursB() {
		DateTime lastCalculated = new DateTime(2009, 6, 22, 12, 5, 0, 0);
		DateTime now = new DateTime(2009, 6, 24, 9, 5, 0, 0);

		Duration elapsedTime = calculator.calculateElapsedTime(lastCalculated, now, SLAWorkingHours.OFFICE_HOURS_9_TO_5_30);
		Assert.assertEquals("PT14H", elapsedTime.toPeriod().toString());
	}

	public void testSpanTwoDaysOfOfficeHoursC() {
		DateTime lastCalculated = new DateTime(2009, 6, 22, 7, 0, 0, 0);
		DateTime now = new DateTime(2009, 6, 23, 19, 0, 0, 0);

		Duration elapsedTime = calculator.calculateElapsedTime(lastCalculated, now, SLAWorkingHours.OFFICE_HOURS_9_TO_5_30);
		Assert.assertEquals("PT17H", elapsedTime.toPeriod().toString());
	}

	public void testSpanTwoDaysOfOfficePlusWeekendHours() {
		DateTime lastCalculated = new DateTime(2009, 6, 19, 7, 0, 0, 0);
		DateTime now = new DateTime(2009, 6, 22, 19, 0, 0, 0);

		Duration elapsedTime = calculator.calculateElapsedTime(lastCalculated, now, SLAWorkingHours.OFFICE_HOURS_9_TO_5_30);
		Assert.assertEquals("PT17H", elapsedTime.toPeriod().toString());
	}

	public void testSpanWeekendAndBankHoliday() {
		DateTime lastCalculated = new DateTime(2008, 12, 31, 7, 0, 0, 0);
		DateTime now = new DateTime(2009, 1, 5, 19, 0, 0, 0);

		Duration elapsedTime = calculator.calculateElapsedTime(lastCalculated, now, SLAWorkingHours.OFFICE_HOURS_9_TO_5_30);
		Assert.assertEquals("PT25H30M", elapsedTime.toPeriod().toString());
	}

	public void testOutOfOfficeHours() {
		DateTime lastCalculated = new DateTime(2009, 6, 19, 19, 0, 0, 0);
		DateTime now = new DateTime(2009, 6, 19, 21, 0, 0, 0);

		Duration elapsedTime = calculator.calculateElapsedTime(lastCalculated, now, SLAWorkingHours.OFFICE_HOURS_9_TO_5_30);
		Assert.assertEquals("PT0S", elapsedTime.toPeriod().toString());
	}
	
	public void test247SLAA() {
		DateTime lastCalculated = new DateTime(2009, 6, 19, 19, 0, 0, 0);
		DateTime now = new DateTime(2009, 6, 19, 21, 0, 0, 0);

		Duration elapsedTime = calculator.calculateElapsedTime(lastCalculated, now, SLAWorkingHours.TWENTYFOUR_SEVEN_365);
		Assert.assertEquals("PT2H", elapsedTime.toPeriod().toString());
	}

	public void test247SLAB() {
		DateTime lastCalculated = new DateTime(2009, 6, 19, 19, 0, 0, 0);
		DateTime now = new DateTime(2009, 6, 26, 21, 5, 0, 0);

		Duration elapsedTime = calculator.calculateElapsedTime(lastCalculated, now, SLAWorkingHours.TWENTYFOUR_SEVEN_365);
		Assert.assertEquals("PT170H5M", elapsedTime.toPeriod().toString());
	}
	
	public void testStringToDurationConversion() {
		
		final String responseSLA = "PT8H30M";
		final Period period = ISOPeriodFormat.standard().parsePeriod(responseSLA);
		final Duration duration = period.toDurationFrom(new DateTime());
		System.out.println(duration);
	}
	

}