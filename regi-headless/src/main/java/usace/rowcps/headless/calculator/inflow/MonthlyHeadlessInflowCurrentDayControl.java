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
import java.util.Calendar;
import java.util.Date;
import java.util.NavigableSet;
import java.util.TimeZone;
import usace.rowcps.regi.model.ManagerId;

/**
 *
 * @author @author <a href="mailto:ryanm@rmanet.com">Ryan A. Miles (ryanm@rmanet.com)</a>
 */
public class MonthlyHeadlessInflowCurrentDayControl extends HeadlessInflowCurrentDayControl
{
	private final Date _startDate;
	private final int _lookForward;

	public MonthlyHeadlessInflowCurrentDayControl(ManagerId manId, Date currentDate, TimeZone projectTimeZone, LocationTemplate locRef)
			throws DbException, DbConnectionException, DbIoException, DataSetTxIllegalArgumentException, DataSetException
	{
		Calendar startCal = Calendar.getInstance(projectTimeZone);
		Calendar endCal = Calendar.getInstance(projectTimeZone);
		
		NavigableSet<Date> dates = getDatesInMonthWithElevation(manId, currentDate, projectTimeZone, locRef);

		startCal.setTime(dates.first());
		endCal.setTime(dates.last());

		_lookForward = endCal.get(Calendar.DATE) - startCal.get(Calendar.DATE);
		_startDate = dates.first();
		setDates(dates);
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
