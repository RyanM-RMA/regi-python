package usace.rowcps.headless.calculator.poolpercent;

import hec.data.location.LocationTemplate;
import hec.db.DbConnectionException;
import hec.db.DbIoException;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import usace.rowcps.headless.calculator.AbstractScriptableCalc;
import usace.rowcps.headless.interfaces.ScriptableCalc;
import usace.rowcps.computation.pool.DbCommitPoolCalc;
import usace.rowcps.regi.model.CacheUsage;
import usace.rowcps.regi.model.ManagerId;
import usace.rowcps.regi.model.RegiDomain;

/**
 *
 * @author ryan
 */
public class ScriptablePoolPercentImpl extends AbstractScriptableCalc implements ScriptableCalc
{
	private static final Logger logger = Logger.getLogger(ScriptablePoolPercentImpl.class.getName());
	public ScriptablePoolPercentImpl(RegiDomain regiDomain, ManagerId managerId)
	{
		super(regiDomain, managerId);
	}

	public void calculatePoolPercents(String officeId, String locationStr, Date startDate, Date endDate)
	{
		DbCommitPoolCalc poolCalc = new DbCommitPoolCalc();

		LocationTemplate locRef = new LocationTemplate(officeId, locationStr);
		try {
			logger.log(Level.INFO, "Calculating Pool Percents for {0} from: {1} to: {2}", new Object[]{locRef, startDate, endDate});
			poolCalc.calcTimeSeries(getRegiDomain(), getManagerId(), locRef, startDate, endDate, CacheUsage.NORMAL);
			logger.log(Level.INFO, "Calculated Pool Percents for {0} from: {1} to: {2}", new Object[]{locRef, startDate, endDate});
		} catch (DbConnectionException | DbIoException ex) {
			logger.log(Level.SEVERE, null, ex);
		}
	}

}
