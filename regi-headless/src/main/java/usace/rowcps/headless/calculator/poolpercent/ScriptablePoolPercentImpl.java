package usace.rowcps.headless.calculator.poolpercent;

import hec.data.location.LocationTemplate;
import hec.data.TimeWindow;
import hec.db.DbConnectionException;
import hec.db.DbIoException;
import java.util.Date;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import usace.metrics.services.Metrics;
import usace.rowcps.headless.calculator.AbstractScriptableCalc;
import usace.rowcps.headless.interfaces.ScriptableCalc;
import usace.rowcps.computation.pool.DbCommitPoolCalc;
import usace.rowcps.data.pool.RegiPool;
import usace.rowcps.data.pool.DbPool;
import usace.rowcps.data.pool.PoolTimeSeries;
import usace.rowcps.metrics.RegiMetricsService;
import usace.rowcps.regi.model.CacheUsage;
import usace.rowcps.regi.model.ManagerId;
import usace.rowcps.regi.model.OptionalParams;
import usace.rowcps.regi.model.RegiDomain;
import usace.rowcps.regi.pool.AtPoolManager;
import usace.rowcps.regi.status.AtProjectManager;

/**
 *
 * @author ryan
 */
public class ScriptablePoolPercentImpl extends AbstractScriptableCalc implements ScriptableCalc
{

	private static final Logger LOGGER = Logger.getLogger(ScriptablePoolPercentImpl.class.getName());

	public ScriptablePoolPercentImpl(RegiDomain regiDomain, ManagerId managerId)
	{
		super(regiDomain, managerId);
	}

	public void calculatePoolPercents(String officeId, String locationStr, Date startDate, Date endDate)
	{
		LocationTemplate locRef = new LocationTemplate(officeId, locationStr);

		Metrics metrics = RegiMetricsService.createMetrics(this.getClass().getSimpleName(), "calculatePoolPercents");
		OptionalParams funcParams = new OptionalParams(metrics);

		LOGGER.log(Level.INFO, "Calculating Pool Percents for {0} from: {1} to: {2}", new Object[]{locRef, startDate, endDate});
		DbCommitPoolCalc poolCalc = new DbCommitPoolCalc();
		AtPoolManager poolMan = regiDomain.getAtPoolManager(managerId);
		
		try
		{
			Set<RegiPool> pools = poolMan.retrievePools(locRef, CacheUsage.NORMAL, funcParams);
			pools.stream()
					.filter(pool -> pool.getTsId() == null)
					.filter(DbPool.class::isInstance)
					.map(DbPool.class::cast)
					.forEach(pool -> pool.getMetaData().setTsId(retrieveDefaultTsId(pool)));

			TimeWindow tw = new TimeWindow(startDate, true, endDate, true);

			poolCalc.calcTimeSeries(getRegiDomain(), getManagerId(), pools, tw, funcParams);

			LOGGER.log(Level.INFO, "Calculated Pool Percents for {0} from: {1} to: {2}", new Object[]{locRef, startDate, endDate});
		}
		catch (DbConnectionException | DbIoException ex)
		{
			LOGGER.log(Level.SEVERE, "Unable to calculate pool time series data.", ex);
		}
	}

	// Based on method from PoolsPanel
	public String retrieveDefaultTsId(RegiPool pool)
	{
		LocationTemplate template = pool.getLocationRef();
		String output = AtProjectManager.getDefaultPoolTimeSeriesIdMask(template);
		AtProjectManager projMan = regiDomain.getAtProjectManager(managerId);
		try
		{
			output = projMan.retrievePoolTimeSeriesIdMask(template);
		}
		catch (DbConnectionException | DbIoException ex)
		{
			LOGGER.log(Level.SEVERE, "Unable to retrieve elevation time series association for " + template.getLocationId() + ".  Defaulting to " + output, ex);
		}
		
		String poolName = pool.getPoolName();
		if (poolName.length() > PoolTimeSeries.MAX_POOL_NAME_LENGTH)
		{
			poolName = poolName.substring(0, PoolTimeSeries.MAX_POOL_NAME_LENGTH);
		}
		
		return output.replace(AtProjectManager.POOL_NAME_MASK, poolName);
	}
}
