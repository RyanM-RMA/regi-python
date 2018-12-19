/*
 * Copyright (c) 2018
 * United States Army Corps of Engineers - Hydrologic Engineering Center (USACE/HEC)
 * All Rights Reserved.  USACE PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval from HEC
 */
package usace.rowcps.headless.calculator.inflow;

import hec.data.DataSetException;
import hec.data.location.LocationTemplate;
import hec.data.tx.DataSetTxIllegalArgumentException;
import hec.db.DbConnectionException;
import hec.db.DbException;
import hec.db.DbIoException;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.NavigableSet;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import usace.rowcps.regi.model.ManagerId;

/**
 *
 * @author @author <a href="mailto:ryanm@rmanet.com">Ryan A. Miles (ryanm@rmanet.com)</a>
 */
public class DailyHeadlessInflowCurrentDayControl extends HeadlessInflowCurrentDayControl
{

	private static final Logger LOGGER = Logger.getLogger(DailyHeadlessInflowCurrentDayControl.class.getName());
	private final Date _startDate;
	private final int _lookForward;

	public DailyHeadlessInflowCurrentDayControl(ManagerId manId, Date startDate, Date endDate, TimeZone projectTimeZone, LocationTemplate locRef)
			throws DbException, DbConnectionException, DbIoException, DataSetTxIllegalArgumentException, DataSetException
	{
		NavigableSet<Date> dates = new TreeSet<>(getDatesInMonthWithElevation(manId, startDate, endDate, projectTimeZone, locRef));
		NavigableSet<Date> filteredDates = dates.subSet(startDate, true, endDate, true);
		//This was added because using false can cause out of range exceptions.
		filteredDates.remove(startDate);
		setDates(filteredDates);

		Date realStartDate = startDate;
		int lookForward = 0;

		if (!filteredDates.isEmpty())
		{
			realStartDate = filteredDates.first();
			Instant startDateInstant = realStartDate.toInstant();
			Instant endDateInstant = filteredDates.last().toInstant();
			Duration timeBetween = Duration.between(startDateInstant, endDateInstant);
			
			lookForward = (int)timeBetween.toDays();
		}
		else
		{
			LOGGER.log(Level.INFO, "Could not find elevation data in date range: "
					+ "\n{0} - {1}"
					+ "\nFor project: {2}"
					+ "\n"
					+ "\nUsing start date of {0} with lookforward of 0 days.",
					new Object[]
					{
						startDate, endDate, locRef.getLocationId()
					});
		}

		_startDate = realStartDate;
		_lookForward = lookForward;
	}

	@Override
	public Date getCurrentDate()
	{
		return _startDate;
	}

	@Override
	public int getLookForwardDays()
	{
		return _lookForward;
	}

}
