package usace.rowcps.headless.calculator.flowgroup;

import hec.data.DataSetException;
import hec.data.location.LocationGroup;
import hec.data.location.LocationTemplate;
import hec.db.DbConnectionException;
import hec.db.DbIoException;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import usace.rowcps.headless.calculator.AbstractScriptableCalc;
import usace.rowcps.headless.interfaces.ScriptableCalc;
import usace.rowcps.computation.ITimeSeriesComputationResult;
import usace.rowcps.computation.flowgroup.DbCommitFlowGroupCalc;
import usace.rowcps.computation.flowgroup.FlowGroupCalc;
import usace.rowcps.computation.flowgroup.FlowGroupComputationException;
import usace.rowcps.data.flowgroup.FlowGroupTimeSeries;
import usace.rowcps.data.flowgroup.IFlowGroup;
import usace.rowcps.regi.model.AtFlowGroupManager;
import usace.rowcps.regi.model.AtTimeSeriesManager;
import usace.rowcps.regi.model.CacheUsage;
import usace.rowcps.regi.model.ManagerId;
import usace.rowcps.regi.model.RegiDomain;

/**
 *
 * @author ryan
 */
public class ScriptableGateFlowImpl extends AbstractScriptableCalc implements ScriptableGateFlowCalc, ScriptableCalc
{
	private static final Logger logger = Logger.getLogger(ScriptableGateFlowImpl.class.getName());


	public ScriptableGateFlowImpl(RegiDomain regiDomain, ManagerId manId)
	{
		super(regiDomain, manId);
	}

	@Override
	public void computeAll(String officeId, String locationId, Date start, Date end)
	{

		long startTime = start.getTime();
		long endTime = end.getTime();
		computeAll(officeId, locationId, startTime, endTime);

	}

	@Override
	public void computeAll(String officeId, String locationId, long startTime, long endTime)
	{
		LocationTemplate projectLocRef = new LocationTemplate(officeId, locationId);

		AtFlowGroupManager flowGroupManager = regiDomain.getAtFlowGroupManager(getManagerId());
		AtTimeSeriesManager atTimeSeriesManager= regiDomain.getAtTimeSeriesManager(getManagerId());

		try {
			Map<IFlowGroup, LocationGroup> conduitGateFlowGroupMap = flowGroupManager.retrieveFlowGroups(projectLocRef,
				null, CacheUsage.NORMAL);

			if (conduitGateFlowGroupMap != null && !conduitGateFlowGroupMap.isEmpty()) {
				final int groupMapSize = conduitGateFlowGroupMap.size();
				logger.log(Level.INFO, "Found {0} groups to compute.", groupMapSize);
				Set<Map.Entry<IFlowGroup, LocationGroup>> entrySet = conduitGateFlowGroupMap.entrySet();
				if (entrySet != null && !entrySet.isEmpty()) {
					FlowGroupCalc flowGroupCalc = new FlowGroupCalc();
				
					int count = 0;
					for (Map.Entry<IFlowGroup, LocationGroup> entry : entrySet) {
						IFlowGroup flowGroup = entry.getKey();
						LocationGroup locationGroup = entry.getValue();
						count ++;
						try {
						//				FlowGroupTimeSeries newFlowGroupTimeSeries = flowGroup.newFlowGroupTimeSeries(parameterType,
							//			interval, duration, version,
							//			intervalOffsetSeconds, startCal.getTime(), null);
							logger.log(Level.INFO, "Computing {0}/{1} Group:{2}", new Object[]{count, groupMapSize, flowGroup.getId()});
							Map<FlowGroupTimeSeries, ITimeSeriesComputationResult> calcTimeSeries = flowGroupCalc.
								calcTimeSeries(getManagerId(), flowGroup,  startTime, endTime);
							if(calcTimeSeries == null){
								// I think that this means there was an error...
								logger.log(Level.INFO, "Compute {0}/{1} Group:{2} returned null.", new Object[]{count, groupMapSize, flowGroup.getId()});
							} else {
								DbCommitFlowGroupCalc.storeToTimeSeriesManager(calcTimeSeries, atTimeSeriesManager);
								logger.log(Level.INFO, "Compute {0}/{1} Group:{2} completed normally.", new Object[]{count, groupMapSize, flowGroup.getId()});
							}
							//ITimeSeriesComputationResult computationResult = calcTimeSeries.get(newFlowGroupTimeSeries);

						} catch (FlowGroupComputationException | DataSetException ex) {
							String message = String.format("Scripted compute for %i/%i group:%s encountered an exception.", new Object[]{count, groupMapSize, flowGroup.getId()});
							Logger.getLogger(ScriptableGateFlowImpl.class.getName()).log(Level.SEVERE, message, ex);
						} 
					}
				}
			}
		} catch (DbConnectionException | DbIoException ex) {
			Logger.getLogger(ScriptableGateFlowImpl.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	@Override
	public void computeAll(String officeId, String[] locationIds, Date start, Date end)
	{
		for (String locationId : locationIds) {
			computeAll(officeId, locationId, start, end);
		}
	}

	@Override
	public void computeAll(String officeId, String[] locationIds, long startTime, long endTime)
	{
		for (String locationId : locationIds) {
			computeAll(officeId, locationId, startTime, endTime);
		}
	}

	@Override
	public void computeFlowGroup(String officeId, String locationId, Date start, Date end, String groupId)
	{
		long startTime = start.getTime();
		long endTime = end.getTime();
		computeFlowGroup(officeId, locationId, startTime, endTime, groupId);
	}

	@Override
	public void computeFlowGroup(String officeId, String locationId, long startTime, long endTime, String groupId)
	{
		AtFlowGroupManager flowGroupManager = regiDomain.getAtFlowGroupManager(getManagerId());
		AtTimeSeriesManager atTimeSeriesManager= regiDomain.getAtTimeSeriesManager(getManagerId());

		LocationTemplate projectLocRef = new LocationTemplate(officeId, locationId);
		try {
			Map<IFlowGroup, LocationGroup> conduitGateFlowGroupMap = flowGroupManager.retrieveFlowGroups(projectLocRef,
				null, CacheUsage.NORMAL);

			Set<Map.Entry<IFlowGroup, LocationGroup>> entrySet = conduitGateFlowGroupMap.entrySet();

			FlowGroupCalc flowGroupCalc = new FlowGroupCalc();
			for (Map.Entry<IFlowGroup, LocationGroup> entry : entrySet) {
				IFlowGroup flowGroup = entry.getKey();
				LocationGroup locationGroup = entry.getValue();

				if (flowGroup.getId().equals(groupId)) {

					try {
						Map<FlowGroupTimeSeries, ITimeSeriesComputationResult> calcTimeSeries = flowGroupCalc.
							calcTimeSeries(getManagerId(), flowGroup, startTime, endTime);

						DbCommitFlowGroupCalc.storeToTimeSeriesManager(calcTimeSeries, atTimeSeriesManager);
						logger.log(Level.INFO, "Compute Group:{2} completed normally.", new Object[]{ flowGroup.getId()});
					} catch (FlowGroupComputationException | DataSetException ex) {
						String message = String.format("Scripted compute for group:%s encountered an exception.", new Object[]{ flowGroup.getId()});
						Logger.getLogger(ScriptableGateFlowImpl.class.getName()).log(Level.WARNING, message, ex);
					}
				}
			}

		} catch (DbConnectionException | DbIoException ex) {
			Logger.getLogger(ScriptableGateFlowImpl.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	@Override
	public void computeFlowGroup(String officeId, String[] locationIds, Date start, Date end, String groupId)
	{
		long startTime = start.getTime();
		long endTime = end.getTime();
		for (String locationId : locationIds) {
			computeFlowGroup(officeId, locationId, startTime, endTime, groupId);
		}
	}

	@Override
	public void computeFlowGroup(String officeId, String[] locationIds, long startTime, long endTime, String groupId)
	{
		for (String locationId : locationIds) {
			computeFlowGroup(officeId, locationId, startTime, endTime, groupId);
		}
	}

}
