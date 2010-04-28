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
import java.util.HashSet;
import java.util.Set;

import org.joda.time.DateMidnight;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.Duration;
import org.joda.time.Interval;

/**
 * This class calculates the SLA time elapsed between to given points in time.
 * SLAWorkingHours constants should be used to specify which SLA working hours
 * should be used in the calculation.
 * 
 * @see SLAWorkingHours
 * @author Adrian Pillinger
 * 
 */
public class SLATimeCalculator {
	private static Set bankHolidays = new HashSet();

	// specifiy the set of bank holidays
	static {
		bankHolidays.add(new DateMidnight(2009, 1, 1)); // New Year's Day
		bankHolidays.add(new DateMidnight(2009, 4, 10)); // Good Friday
		bankHolidays.add(new DateMidnight(2009, 4, 13)); // Easter Monday
		bankHolidays.add(new DateMidnight(2009, 5, 4)); // Early May Bank Holiday
		bankHolidays.add(new DateMidnight(2009, 5, 25)); // Spring Bank Holiday
		bankHolidays.add(new DateMidnight(2009, 8, 31)); // Summer Bank Holiday
		bankHolidays.add(new DateMidnight(2009, 12, 25)); // Christmas Day
		bankHolidays.add(new DateMidnight(2009, 12, 28)); // Boxing Day

		bankHolidays.add(new DateMidnight(2010, 1, 1)); // New Year's Day
		bankHolidays.add(new DateMidnight(2010, 4, 2)); // Good Friday
		bankHolidays.add(new DateMidnight(2010, 4, 5)); // Easter Monday
		bankHolidays.add(new DateMidnight(2010, 5, 3)); // Early May Bank Holiday
		bankHolidays.add(new DateMidnight(2010, 5, 31)); // Spring Bank Holiday
		bankHolidays.add(new DateMidnight(2010, 8, 30)); // Summer Bank Holiday
		bankHolidays.add(new DateMidnight(2010, 12, 27)); // Christmas Day
		bankHolidays.add(new DateMidnight(2010, 12, 28)); // Boxing Day

		bankHolidays.add(new DateMidnight(2011, 1, 3)); // New Year's Day
		bankHolidays.add(new DateMidnight(2011, 4, 22)); // Good Friday
		bankHolidays.add(new DateMidnight(2011, 4, 25)); // Easter Monday
		bankHolidays.add(new DateMidnight(2011, 5, 2)); // Early May Bank Holiday
		bankHolidays.add(new DateMidnight(2011, 5, 30)); // Spring Bank Holiday
		bankHolidays.add(new DateMidnight(2011, 8, 29)); // Summer Bank Holiday
		bankHolidays.add(new DateMidnight(2011, 12, 26)); // Boxing Day
		bankHolidays.add(new DateMidnight(2011, 12, 27)); // Christmas Day
	}
	/**
	 * Calculate the elapsed time since a lasted calculated date and an end
	 * calculation time, based upon the SLA working hours constant provided.
	 * 
	 * @param lastCalculated
	 * @param now
	 * @param slaWorkingHours
	 * @return
	 */
	public Duration calculateElapsedTime(Timestamp lastCalculated, Timestamp now,
			int slaWorkingHours) {
		DateTime lastCalculatedDT = new DateTime(lastCalculated);
		DateTime nowDT = new DateTime(now);
		return calculateElapsedTime(lastCalculatedDT, nowDT, slaWorkingHours);
	}
	/**
	 * Calculate the elapsed time since a lasted calculated date and an end
	 * calculation time, based upon the SLA working hours constant provided.
	 * 
	 * @param lastCalculated
	 * @param now
	 * @param slaWorkingHours
	 * @return
	 */
	public Duration calculateElapsedTime(DateTime lastCalculated, DateTime now,
			int slaWorkingHours) {
		DateMidnight dateBeingProcessed = new DateMidnight(lastCalculated);
		Interval processingInterval = getEntireDayIntervalForDate(dateBeingProcessed);
		Duration duration = new Duration(null);
		while (processingInterval.contains(now)
				|| processingInterval.isBefore(now)) {
			Interval workingHours = getWorkingIntervalForDate(
					dateBeingProcessed, slaWorkingHours);

			// process the day
			// if the last calculated date is within the current 24hours being
			// processed.
			if (processingInterval.contains(lastCalculated)) {
				// set start to last calc'd or start or working period if after
				// last calced
				if (workingHours.getStart().isAfter(lastCalculated)) {
					// leave start of working hours as is
				} else if (workingHours.contains(lastCalculated)) {
					workingHours = workingHours.withStart(lastCalculated);
				} else if (workingHours.getEnd().isBefore(lastCalculated)) {
					workingHours = new Interval(lastCalculated, lastCalculated); // zero
					// time
					// duration
				}
			}
			if (processingInterval.contains(now)) {
				// set end to now if now in working period
				if (workingHours.getStart().isAfter(now)) {
					workingHours = new Interval(now, now); // zero time duration
				} else if (workingHours.contains(now)) {
					workingHours = workingHours.withEnd(now);
				} else if (workingHours.getEnd().isBefore(now)) {
					// leave end of working hours as is
				}
			}
			duration = duration.plus(workingHours.toDuration());
			dateBeingProcessed = dateBeingProcessed.plusDays(1);
			processingInterval = getEntireDayIntervalForDate(dateBeingProcessed);
		}
		System.out.println(duration.toPeriod());
		return duration;
	}

	/**
	 * Create an interval for an entire day
	 * 
	 * @param start
	 * @return
	 */
	private Interval getEntireDayIntervalForDate(DateMidnight start) {
		final DateMidnight end = start.plusDays(1);
		Interval interval = new Interval(start, end);
		return interval;
	}

	/**
	 * Calculate the working interval for the SLA for the given date and sla
	 * working hours
	 * 
	 * @param start
	 * @param slaWorkingHours
	 * @return
	 */
	private Interval getWorkingIntervalForDate(DateMidnight start,
			int slaWorkingHours) {
		Interval interval;
		// sla running all the time.
		if (slaWorkingHours == SLAWorkingHours.TWENTYFOUR_SEVEN_365) {
			interval = getEntireDayIntervalForDate(start);
		}
		// sla running only in office hours
		else if (slaWorkingHours == SLAWorkingHours.OFFICE_HOURS_9_TO_5_30) {
			final int dayOfWeek = start.getDayOfWeek();
			// if sat/sun/bank hol
			if (dayOfWeek == DateTimeConstants.SATURDAY
					|| dayOfWeek == DateTimeConstants.SUNDAY
					|| bankHolidays.contains(start)) // bank holiday or weekend
			{
				interval = new Interval(start, start); // 0 duration interval
			}
			// else it is a working day 9-5.30 office hours
			else {
				DateTime startOfficeHours = new DateTime(start.getYear(), start
						.getMonthOfYear(), start.getDayOfMonth(), 9, 0, 0, 0);
				DateTime endOfficeHours = new DateTime(start.getYear(), start
						.getMonthOfYear(), start.getDayOfMonth(), 17, 30, 0, 0);
				interval = new Interval(startOfficeHours, endOfficeHours);
			}
		} else // unknown SLA working hours
		{
			// TODO log warning that SLA time isn't running
			interval = new Interval(start, start); // 0 duration interval
		}
		return interval;
	}
}
