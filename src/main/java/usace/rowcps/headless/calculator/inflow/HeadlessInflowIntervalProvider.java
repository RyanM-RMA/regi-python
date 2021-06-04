/*
 * Copyright (c) 2018
 * United States Army Corps of Engineers - Hydrologic Engineering Center (USACE/HEC)
 * All Rights Reserved.  USACE PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval from HEC
 */
package usace.rowcps.headless.calculator.inflow;

import hec.data.DataSetIllegalArgumentException;
import hec.data.Interval;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import usace.rowcps.computation.common.IntervalProvider;

/**
 *
 * @author @author <a href="mailto:ryanm@rmanet.com">Ryan A. Miles (ryanm@rmanet.com)</a>
 */
public class HeadlessInflowIntervalProvider implements IntervalProvider
{
	private static final Logger LOGGER = Logger.getLogger(HeadlessInflowIntervalProvider.class.getName());
	private final long _msTimeOffsetIntoInterval;
	private final TimeZone _projectTimeZone;
	private final String _intervalName;

	public HeadlessInflowIntervalProvider(long _msTimeOffsetIntoInterval, TimeZone _projectTimeZone, String _intervalName)
	{
		this._msTimeOffsetIntoInterval = _msTimeOffsetIntoInterval;
		this._projectTimeZone = _projectTimeZone;
		this._intervalName = _intervalName;
	}

	@Override
	public boolean isPeriodAverage()
	{
		return false;
	}

	@Override
	public Interval getInterval()
	{
		Interval interval = null;
		try
		{
			if (_intervalName == null)
			{
				interval = new Interval("1Day");
			}
			else
			{
				interval = new Interval(_intervalName);
			}
		}
		catch (DataSetIllegalArgumentException ex)
		{
			LOGGER.log(Level.WARNING, "Error instantiating interval for: " + _intervalName + ".", ex);
		}

		return interval;
	}

	@Override
	public int getIntervalOffsetSeconds()
	{
		return (int) (_msTimeOffsetIntoInterval / 1000L);
	}

}
