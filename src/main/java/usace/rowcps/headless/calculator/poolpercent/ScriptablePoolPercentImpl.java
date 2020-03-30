package usace.rowcps.headless.calculator.poolpercent;

import hec.data.Duration;
import hec.data.Interval;
import hec.data.ParameterType;
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
import usace.rowcps.data.pool.IPool;
import usace.rowcps.metrics.RegiMetricsService;
import usace.rowcps.regi.model.AtLocationLevelManager;
import usace.rowcps.regi.model.CacheUsage;
import usace.rowcps.regi.model.ManagerId;
import usace.rowcps.regi.model.OptionalParams;
import usace.rowcps.regi.model.RegiDomain;

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
		OptionalParams options = new OptionalParams(metrics);

		LOGGER.log(Level.INFO, "Calculating Pool Percents for {0} from: {1} to: {2}", new Object[]{locRef, startDate, endDate});
		DbCommitPoolCalc poolCalc = new DbCommitPoolCalc();
		AtLocationLevelManager atLocLevelMgr = regiDomain.getAtLocationLevelManager(managerId);
		try {
			Set<IPool> pools = atLocLevelMgr.retrievePools(locRef, CacheUsage.NORMAL);

			//calculate and save the pool storages
			for (IPool pool : pools) {

				if (pool.getTsId() == null) {
					// this is what PoolPanel would do:
					pool.setTsId(defaultTsId(pool));
				}
			}
			
			TimeWindow tw = new TimeWindow(startDate, true, endDate, true);

			poolCalc.calcTimeSeries(getRegiDomain(), getManagerId(), pools, tw, options);

			LOGGER.log(Level.INFO, "Calculated Pool Percents for {0} from: {1} to: {2}", new Object[]{locRef, startDate, endDate});
		} catch (DbConnectionException | DbIoException ex) {
			LOGGER.log(Level.SEVERE, "Unable to calculate pool time series data.", ex);
		}
	}

	// Based on method from PoolsPanel
	public static String defaultTsId(IPool pool)
	{
		String location = pool.getLocationRef().getLocationId();
		String parameter = "%-Stor" + "," + pool.getIdSuffix();
		String type = ParameterType.getAvailableParameterTypes()[0];
		String interval = Interval.getAvailableIntervals()[0];
		String duration = Duration.getAvailableDurations()[0];
		String version = "Computed";

		return location + "." + parameter + "." + type + "." + interval + "." + duration + "." + version;
	}
}
