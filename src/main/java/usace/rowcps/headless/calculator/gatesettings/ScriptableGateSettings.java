package usace.rowcps.headless.calculator.gatesettings;

import hec.data.location.LocationTemplate;
import java.util.Date;
import java.util.Set;
import usace.rowcps.headless.calculator.AbstractScriptableCalc;
import usace.rowcps.headless.interfaces.ScriptableCalc;
import usace.rowcps.computation.common.IThreadedBlockRetriever;
import usace.rowcps.regi.model.ManagerId;
import usace.rowcps.regi.model.RegiDomain;

/**
 *
 * @author ryan
 */
class ScriptableGateSettings extends AbstractScriptableCalc implements ScriptableCalc
{

	public ScriptableGateSettings(RegiDomain regiDomain, ManagerId managerId)
	{
		super(regiDomain, managerId);
	}

	public void adjustGateSettings(String officeId, String locationStr, Date startDate, double val, double unit)
	{
		LocationTemplate locRef = new LocationTemplate(officeId, locationStr);


		  Set<Date> modifiedDatesForCachedSettings = null;
//
		   IThreadedBlockRetriever completionCallbackTarget = null;
		    final int MAXROWSTORETRIEVE = 35;

//        GateCache commonGateCache = new GateCache(currentDayControl, getManagerId(), projectDescriptor, MAXROWSTORETRIEVE, completionCallbackTarget,
//                modifiedDatesForCachedSettings);
//
//        commonGateCache.initCache();

	
	}

}
