/*
 * Copyright (c) 2018
 * United States Army Corps of Engineers - Hydrologic Engineering Center (USACE/HEC)
 * All Rights Reserved.  USACE PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval from HEC
 */
package usace.rowcps.headless.calculator.inflow;

import hec.data.project.AtProjectDescriptor;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.NavigableSet;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import usace.rowcps.computation.common.IntervalProvider;
import usace.rowcps.computation.inflow.InflowCache;
import usace.rowcps.computation.inflow.InflowDataAdapter;
import usace.rowcps.data.CacheInitializationException;
import usace.rowcps.data.inflow.InflowDataContainer;
import usace.rowcps.data.inflow.InflowDataType;
import usace.rowcps.regi.executor.DefaultThreadIdProvider;
import usace.rowcps.regi.model.ManagerId;
import usace.rowcps.regi.model.OptionalParams;

/**
 * This class is intended to extend the cache, and support functions for headless operations.
 *
 * @author @author <a href="mailto:ryanm@rmanet.com">Ryan A. Miles (ryanm@rmanet.com)</a>
 */
public class HeadlessInflowCache extends InflowCache
{

	private static final Logger LOGGER = Logger.getLogger(HeadlessInflowCache.class.getName());

	public HeadlessInflowCache(HeadlessInflowCurrentDayControl currentDayControl, ManagerId managerId,
								AtProjectDescriptor projectDescriptor, TimeZone projectTimeZone,
								IntervalProvider intervalProvider, InflowDataAdapter adapter)
	{
		super(currentDayControl, managerId, projectDescriptor, null, new InternalThreadBlockRetriever(), new HashSet<>(), projectTimeZone, intervalProvider, adapter);
	}

	@Override
	public HeadlessInflowCurrentDayControl getCurrentDayControl()
	{
		return (HeadlessInflowCurrentDayControl) super.getCurrentDayControl();
	}

	/**
	 * This method is used to initialize the cache and then wait for the initialization to complete via the
	 * InternalThreadBlockRetriever private internal class.
	 *
	 * @param params
	 * @throws CacheInitializationException
	 */
	public final void waitForInitCache(OptionalParams params) throws CacheInitializationException
	{
		initCache(new DefaultThreadIdProvider(), params);
		InternalThreadBlockRetriever callback = (InternalThreadBlockRetriever) getCompletionCallbackTarget();
		try
		{
			Integer seconds = Integer.getInteger("rowcps.latchseconds", 11 * 60);
			callback.getLatch().await(seconds, TimeUnit.SECONDS);
		}
		catch (InterruptedException ex)
		{
			throw new CacheInitializationException(ex);
		}

		NavigableSet<Date> dates = getDateRangeFromControl();
		for (Date date : dates)
		{
			computedInflowForDate(date, hec.data.Units.ENGLISH_ID, getProjectTimeZone());
			computeInflowMassDelta(date, hec.data.Units.ENGLISH_ID);
		}
	}

	private NavigableSet<Date> getDateRangeFromControl()
	{
		Date currentDate = getCurrentDayControl().getCurrentDate();
		int lookForwardDays = getCurrentDayControl().getLookForwardDays();
		Calendar cal = Calendar.getInstance(getProjectTimeZone());
		NavigableSet<Date> output = new TreeSet<>();

		cal.setTime(currentDate);
		output.add(cal.getTime());

		for (int i = 0; i <= lookForwardDays; i++)
		{
			cal.add(Calendar.DAY_OF_MONTH, 1);
			output.add(cal.getTime());
		}

		return output;
	}

	/**
	 * Helper function for clearing the modified state (setModified(false)) for all of the inflow data containers in the
	 * cache for all types.
	 */
	public void clearModifiedState()
	{
		List<Date> dateKeys = getInflowDataDateKeys();
		dateKeys.forEach((date) ->
		{
			for (InflowDataType dataType : InflowDataType.values())
			{
				InflowDataContainer container = getInflowDataContainer(date, dataType);
				if (container != null)
				{
					container.setModified(false);
				}
			}
		});
	}

	/**
	 * Helper function for setting the modified state of all of the inflow data containers in the cache for a given
	 * type.
	 *
	 * @param dataType
	 * @param modifiedState
	 */
	public void setModifiedOnAllDataTypeValuesInTimeRange(InflowDataType dataType, boolean modifiedState)
	{
		List<Date> dateKeys = getInflowDataDateKeys();
		dateKeys.forEach((date) ->
		{
			InflowDataContainer container = getInflowDataContainer(date, dataType);
			if (container != null)
			{
				container.setModified(modifiedState);
			}
		});
	}

	@Override
	public void blockByThreshold(List<Date> datesInRange, double threshold)
	{
		LOGGER.log(Level.FINE, "blockByThreshold is purposefully left empty since it affects the protected status of rows.");
	}
	
	@Override
	public void setQualityForRainDays(Date date)
	{
		LOGGER.log(Level.FINE, "setQualityForRainDays is purposefully left empty since it affects the protected status of rows.");
	}

	@Override
	protected void setQualityForCopyInflow(InflowDataContainer idcNew)
	{
		removeProtectionAndOverride(idcNew);
	}
	
	private void removeProtectionAndOverride(InflowDataContainer idc)
	{
		if (idc != null)
		{
			idc.setProtected(false);
			idc.setProtectedOverride(false);
		}
	}

	@Override
	public void setAdjustedValues(List<Date> datesInRange, double value)
	{
		for(Date dateKey : datesInRange)
		{
			InflowDataContainer idcAdjusted = getInflowDataContainer(dateKey, InflowDataType.AdjustedInflow);
			//Check for protected values, headless shouldn't be changing protected stuff.
			if(idcAdjusted != null && idcAdjusted.getValue() instanceof Double)
			{
				idcAdjusted.setValue(value);
				removeProtectionAndOverride(idcAdjusted);
			}
		}
	}

	@Override
	public void setAdjustedValue(double val, int quality, Date dateKey)
	{
		super.setAdjustedValue(val, quality, dateKey);
		
		InflowDataContainer idcNew = getInflowDataContainer(dateKey, InflowDataType.AdjustedInflow);
		removeProtectionAndOverride(idcNew);
	}

	@Override
	protected boolean cloneComputedInflowIntoAdjusted(InflowDataContainer idcNew, Date rowDate, Double newDouble)
	{
		boolean output = super.cloneComputedInflowIntoAdjusted(idcNew, rowDate, newDouble);
		removeProtectionAndOverride(idcNew);
		return output;
	}

	private static class InternalThreadBlockRetriever extends AbstractThreadedBlockRetriever
	{

		private final CountDownLatch _latch = new CountDownLatch(1);

		public CountDownLatch getLatch()
		{
			return _latch;
		}

		@Override
		public void asyncHeadCacheFetchCompleted()
		{
			LOGGER.info("asyncHeadCacheFetchCompleted");
			_latch.countDown();
		}
	}
}
