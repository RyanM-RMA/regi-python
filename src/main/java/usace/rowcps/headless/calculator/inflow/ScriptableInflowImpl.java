package usace.rowcps.headless.calculator.inflow;

import hec.data.DataSetException;
import hec.data.DataSetIllegalArgumentException;
import hec.data.Interval;
import hec.data.location.LocationTemplate;
import hec.data.project.AtProjectDescriptor;
import hec.db.DbConnectionException;
import hec.db.DbException;
import hec.db.DbIoException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import usace.metrics.services.Metrics;
import usace.metrics.services.MetricsServiceProvider;
import usace.rowcps.computation.common.IntervalProvider;
import usace.rowcps.computation.inflow.AutoAdjustInflowsAction;
import usace.rowcps.computation.inflow.BalanceAdjustedInflowsAction;
import usace.rowcps.computation.inflow.CloneInflowsAction;
import usace.rowcps.computation.inflow.InflowAdjustedTypeModel;
import usace.rowcps.computation.inflow.InflowCache;
import usace.rowcps.computation.inflow.ZeroNegativeAdjustedInflowsAction;
import usace.rowcps.data.CacheInitializationException;
import usace.rowcps.data.LocalOffset;
import usace.rowcps.data.inflow.InflowDataType;
import usace.rowcps.headless.calculator.AbstractScriptableCalc;
import usace.rowcps.headless.interfaces.ScriptableCalc;
import usace.rowcps.regi.model.CacheUsage;
import usace.rowcps.regi.model.ManagerId;
import usace.rowcps.regi.model.OptionalParams;
import usace.rowcps.regi.model.RegiDomain;
import usace.rowcps.regi.status.AtProjectManager;

/**
 *
 * @author ryan
 */
public class ScriptableInflowImpl extends AbstractScriptableCalc implements ScriptableCalc, ScriptableInflow
{

	private static final Logger LOGGER = Logger.getLogger(ScriptableInflowImpl.class.getName());

	private final long _msTimeOffsetIntoInterval = 7 * 3600 * 1000;  //TODO  7am for now
	private final String _intervalName = "1Day";
	private final Map<LocationTemplate, InflowAdjustedTypeModel> _statusMaps = new HashMap<>();
	private InflowStorageOptions _storageOptions = InflowStorageOptions.storeAllComputedData();

	private IntervalProvider buildIntervalProvider(TimeZone projectTimeZone)
	{
		return new IntervalProvider()
		{
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
					LOGGER.log(Level.WARNING,
							"Error instantiating interval for: " + _intervalName + ".",
							ex);
				}

				return interval;
			}

			@Override
			public int getIntervalOffsetSeconds()
			{
				return (int) (_msTimeOffsetIntoInterval / 1000L);
			}

			@Override
			public int getUtcIntervalOffsetSeconds()
			{
				LocalOffset localOffset = new LocalOffset(projectTimeZone, getInterval());
				return localOffset.getUtcOffsetInSeconds();
			}
		};

	}

	public ScriptableInflowImpl(RegiDomain regiDomain, ManagerId managerId)
	{
		super(regiDomain, managerId);
	}

	@Override
	public void autoAdjust(String officeId, String locationStr, Date startDate)
	{
		autoAdjust(officeId, locationStr, startDate, false, false);
	}

	@Override
	public void autoAdjust(String officeId, String locationStr, Date startDate, boolean useLimits, boolean freezeRain)
	{
		Metrics metrics = MetricsServiceProvider.createMetrics(this.getClass().getSimpleName(), "autoAdjust");
		OptionalParams options = new OptionalParams(metrics);
		try
		{
			LocationTemplate locRef = new LocationTemplate(officeId, locationStr);

			InflowAdjustedTypeModel asm = getOrCreateStatusMap(locRef);

			InflowCache inflowCache = getInitializedMonthlyCache(locRef, startDate, options);
			startDate = inflowCache.getCeilingDateKey(startDate);

			LOGGER.info("performing AutoAdjustInflowsAction");

			AutoAdjustInflowsAction aaia = new AutoAdjustInflowsAction(startDate, inflowCache, hec.data.Units.ENGLISH_ID, asm);
			aaia.setFreezeRainDays(freezeRain);
			aaia.setUseLimits(useLimits);

			aaia.actionPerformed(null);
			LOGGER.info("AutoAdjustInflowsAction complete. Saving cache data.");

			inflowCache.saveData(options);

		}
		catch (DbConnectionException | DbIoException ex)
		{
			LOGGER.log(Level.SEVERE, "DB exception occurred while running autoAdjust", ex);
		}
		catch (InterruptedException ie)
		{
			Thread.currentThread().interrupt();
		}
		catch (Exception ex)
		{
			LOGGER.log(Level.SEVERE, "Exception occurred while running autoAdjust", ex);
		}
	}

	@Override
	public void cloneInflows(String officeId, String locationStr, Date startDate)
	{
		Metrics metrics = MetricsServiceProvider.createMetrics(this.getClass().getSimpleName(), "cloneInflows");
		OptionalParams options = new OptionalParams(metrics);

		try
		{
			LocationTemplate locRef = new LocationTemplate(officeId, locationStr);

			InflowAdjustedTypeModel asm = getOrCreateStatusMap(locRef);

			InflowCache inflowCache = getInitializedMonthlyCache(locRef, startDate, options);
			startDate = inflowCache.getCeilingDateKey(startDate);

			LOGGER.info("performing CloneInflowsAction");
			CloneInflowsAction cloneAction
					= new CloneInflowsAction(startDate, inflowCache, hec.data.Units.ENGLISH_ID, asm);
			cloneAction.actionPerformed(null);
			LOGGER.info("CloneInflowsAction completed. Saving cache data.");

			inflowCache.saveData(options);

		}
		catch (DbConnectionException | DbIoException ex)
		{
			LOGGER.log(Level.SEVERE, "DB exception occurred while cloning inflows.", ex);
		}
		catch (Exception ex)
		{
			LOGGER.log(Level.SEVERE, "Unable to clone inflows", ex);
		}
	}

	@Override
	public void zeroNegatives(String officeId, String locationStr, Date startDate)
	{
		Metrics metrics = MetricsServiceProvider.createMetrics(this.getClass().getSimpleName(), "zeroNegatives");
		OptionalParams options = new OptionalParams(metrics);

		try
		{
			LocationTemplate locRef = new LocationTemplate(officeId, locationStr);

			InflowAdjustedTypeModel asm = getOrCreateStatusMap(locRef);

			InflowCache inflowCache = getInitializedMonthlyCache(locRef, startDate, options);
			startDate = inflowCache.getCeilingDateKey(startDate);

			LOGGER.info("performing ZeroNegativeAdjustedInflowsAction");
			ZeroNegativeAdjustedInflowsAction zeroAction = new ZeroNegativeAdjustedInflowsAction(startDate, inflowCache,
					hec.data.Units.ENGLISH_ID, asm);
			zeroAction.actionPerformed(null);
			LOGGER.info("ZeroNegativeAdjustedInflowsAction complete.  Saving cache data.");
			inflowCache.saveData(options);

		}
		catch (DbConnectionException | DbIoException ex)
		{
			LOGGER.log(Level.SEVERE, "DB exception occured while zeroing negatives", ex);
		}
		catch (Exception ex)
		{
			LOGGER.log(Level.SEVERE, "Unable to zero negatives", ex);
		}
	}

	@Override
	public void balanceAll(String officeId, String locationStr, Date startDate)
	{
		Metrics metrics = MetricsServiceProvider.createMetrics(this.getClass().getSimpleName(), "balanceAll");
		OptionalParams options = new OptionalParams(metrics);
		try
		{
			LocationTemplate locRef = new LocationTemplate(officeId, locationStr);

			InflowCache inflowCache = getInitializedMonthlyCache(locRef, startDate, options);
			startDate = inflowCache.getCeilingDateKey(startDate);

			InflowAdjustedTypeModel asm = getOrCreateStatusMap(locRef);

			LOGGER.info("performing BalanceAdjustedInflowsAction");
			BalanceAdjustedInflowsAction balanceAll = new BalanceAdjustedInflowsAction(startDate, inflowCache,
					hec.data.Units.ENGLISH_ID, asm);
			balanceAll.actionPerformed(null);
			LOGGER.info("BalanceAdjustedInflowsAction complete.  Saving data.");

			inflowCache.saveData(options);

		}
		catch (DbConnectionException | DbIoException ex)
		{
			LOGGER.log(Level.SEVERE, "DB Exception occurred while balancing inflow", ex);
		}
		catch (Exception ex)
		{
			LOGGER.log(Level.SEVERE, "Unable to balance inflow", ex);
		}
	}

	private AtProjectDescriptor getProjectDescriptor(LocationTemplate template) throws DbConnectionException, DbIoException
	{
		RegiDomain domain = getRegiDomain();
		AtProjectManager atProjectManager = domain.getAtProjectManager(getManagerId());
		return atProjectManager.getProjectDescriptor(template, CacheUsage.NORMAL);
	}

	private TimeZone getProjectTimeZone(AtProjectDescriptor descriptor) throws DbConnectionException, DbIoException
	{
		RegiDomain domain = getRegiDomain();
		AtProjectManager atProjectManager = domain.getAtProjectManager(getManagerId());
		return atProjectManager.getIProject(descriptor).getProjectTimeZone();
	}

	private HeadlessInflowCache getInitializedDailyCache(LocationTemplate locRef, Date startDate, Date endDate, OptionalParams options)
			throws DbConnectionException, DbIoException, InterruptedException, CacheInitializationException,
				   DbException, DataSetException
	{
		AtProjectDescriptor projectDescriptor = getProjectDescriptor(locRef);
		TimeZone projectTimeZone = getProjectTimeZone(projectDescriptor);
		DailyHeadlessInflowCurrentDayControl dayControl = new DailyHeadlessInflowCurrentDayControl(getManagerId(), startDate, endDate, projectTimeZone, locRef);

		HeadlessInflowCache cache = buildAndInitializeInflowCache(dayControl, projectDescriptor, projectTimeZone, startDate, options);

		return cache;
	}

	private HeadlessInflowCache getInitializedMonthlyCache(LocationTemplate locRef, Date startDate, OptionalParams options)
			throws DbConnectionException, DbIoException, InterruptedException, CacheInitializationException,
				   DbException, DataSetException
	{
		AtProjectDescriptor projectDescriptor = getProjectDescriptor(locRef);
		TimeZone projectTimeZone = getProjectTimeZone(projectDescriptor);
		MonthlyHeadlessInflowCurrentDayControl dayControl = new MonthlyHeadlessInflowCurrentDayControl(getManagerId(), startDate, projectTimeZone, locRef);

		return buildAndInitializeInflowCache(dayControl, projectDescriptor, projectTimeZone, startDate, options);
	}

	private HeadlessInflowCache buildAndInitializeInflowCache(HeadlessInflowCurrentDayControl currentDayControl, AtProjectDescriptor projectDescriptor, TimeZone projectTimeZone, Date startDate, OptionalParams options) throws CacheInitializationException
	{
		HeadlessInflowCache inflowCache = new HeadlessInflowCache(currentDayControl, getManagerId(), projectDescriptor,
				projectTimeZone, getIntervalProvider(projectTimeZone))
		{

			@Override
			protected void updateHeadTimeWindow(Calendar start, Calendar end, SortedMap<Date, ? extends Object> currentCache)
			{
				super.updateHeadTimeWindow(start, end, currentCache);
				if (start.getTime().after(startDate))
				{
					start.setTime(startDate);
				}
			}
		};

		inflowCache.waitForInitCache(options);

		LOGGER.info("InflowCache is initialized.");

		return inflowCache;
	}

	private IntervalProvider getIntervalProvider(TimeZone projectTimeZone)
	{
		return buildIntervalProvider(projectTimeZone);
	}

	private InflowAdjustedTypeModel getOrCreateStatusMap(LocationTemplate template)
	{
		InflowAdjustedTypeModel retval = null;
		if (template != null)
		{
			retval = _statusMaps.get(template);
			if (retval == null)
			{
				retval = new InflowAdjustedTypeModelImpl();
				_statusMaps.put(template, retval);
			}
		}

		return retval;
	}

	@Override
	public void computeEvapAsFlow(String officeId, String locationStr, Date startDate, Date endDate)
	{
		Metrics metrics = MetricsServiceProvider.createMetrics(this.getClass().getSimpleName(), "zeroNegatives");
		OptionalParams options = new OptionalParams(metrics);

		try
		{
			LocationTemplate locRef = new LocationTemplate(officeId, locationStr);
			HeadlessInflowCache cache = getInitializedDailyCache(locRef, startDate, endDate, options);
			
			cache.clearModifiedState();
			cache.setModifiedOnAllDataTypeValuesInTimeRange(InflowDataType.ProjectEvapAsFlow, true);

			cache.saveData(options);
		}
		catch (DbConnectionException | DbIoException ex)
		{
			LOGGER.log(Level.SEVERE, "DB Exception occurred while computing evap as flow", ex);
		}
		catch (Exception ex)
		{
			LOGGER.log(Level.SEVERE, "Unable to compute evap as flow", ex);
		}
	}

	@Override
	public void computeInflow(String officeId, String locationStr, Date startDate, Date endDate)
	{
		Metrics metrics = MetricsServiceProvider.createMetrics(this.getClass().getSimpleName(), "zeroNegatives");
		OptionalParams options = new OptionalParams(metrics);

		try
		{
			LocationTemplate locRef = new LocationTemplate(officeId, locationStr);
			HeadlessInflowCache cache = getInitializedDailyCache(locRef, startDate, endDate, options);

			cache.clearModifiedState();
			cache.setModifiedOnAllDataTypeValuesInTimeRange(InflowDataType.ComputedInflow, true);

			if (_storageOptions.isStoringEvapAsFlow())
			{
				cache.setModifiedOnAllDataTypeValuesInTimeRange(InflowDataType.ProjectEvapAsFlow, true);
			}

			if (_storageOptions.isStoringProjectReleases())
			{
				cache.setModifiedOnAllDataTypeValuesInTimeRange(InflowDataType.AverageRelease, true);
			}

			cache.saveData(options);
		}
		catch (DbConnectionException | DbIoException ex)
		{
			LOGGER.log(Level.SEVERE, "DB Exception occurred while computing inflow", ex);
		}
		catch (Exception ex)
		{
			LOGGER.log(Level.SEVERE, "Unable to compute inflow", ex);
		}
	}

	@Override
	public void setStorageOptions(InflowStorageOptions options)
	{
		if (options == null)
		{
			//Null is implied to be no additional data, so users can use this from the script like:
			//inflowCalc.setStorageOptions(None)
			//And nothing extra is stored.
			options = InflowStorageOptions.doNotStoreAllComputedData();
		}

		_storageOptions = options;
	}
}
