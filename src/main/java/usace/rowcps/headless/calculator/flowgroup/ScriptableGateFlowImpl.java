package usace.rowcps.headless.calculator.flowgroup;

import hec.data.DataSetException;
import hec.data.UtcOffsetConst;
import hec.data.location.LocationGroup;
import hec.data.location.LocationTemplate;
import hec.data.tx.DataSetTx;
import hec.db.DbConnectionException;
import hec.db.DbIoException;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import usace.metrics.services.Metrics;
import usace.metrics.services.MetricsServiceProvider;
import usace.rowcps.computation.ITimeSeriesComputationResult;
import usace.rowcps.computation.TimeSeriesComputationData;
import usace.rowcps.computation.flowgroup.DbCommitFlowGroupCalc;
import usace.rowcps.computation.flowgroup.FlowGroupCalc;
import usace.rowcps.computation.flowgroup.FlowGroupComputationException;
import usace.rowcps.computation.util.FlowGroupOverrideDataAdapterBase;
import usace.rowcps.data.DefaultFlowGroup;
import usace.rowcps.data.flowgroup.FlowGroupTimeSeries;
import usace.rowcps.data.flowgroup.IFlowGroup;
import usace.rowcps.data.project.AtProjectDescriptor;
import usace.rowcps.headless.calculator.AbstractScriptableCalc;
import usace.rowcps.headless.interfaces.ScriptableCalc;
import usace.rowcps.regi.model.AtFlowGroupManager;
import usace.rowcps.regi.model.AtTimeSeriesManager;
import usace.rowcps.regi.model.CacheUsage;
import usace.rowcps.regi.model.ManagerId;
import usace.rowcps.regi.model.OptionalParams;
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

		computeAll(officeId, locationId, start.getTime(), end.getTime());

	}

	@Override
	public void computeAll(String officeId, String locationId, long startTime, long endTime)
	{
		LocationTemplate projectLocRef = new LocationTemplate(officeId, locationId);

		Metrics metrics = MetricsServiceProvider.createMetrics(this.getClass().getSimpleName(), "computeAll");
		OptionalParams options = new OptionalParams(metrics);

		AtFlowGroupManager flowGroupManager = regiDomain.getAtFlowGroupManager(getManagerId());
		AtTimeSeriesManager atTimeSeriesManager= regiDomain.getAtTimeSeriesManager(getManagerId());

		final TimeZone utcZone = null;//TimeZone.getTimeZone("UTC");
		try {
			Map<IFlowGroup, LocationGroup> conduitGateFlowGroupMap = flowGroupManager.retrieveFlowGroups(projectLocRef,	null, CacheUsage.NORMAL);

			if (conduitGateFlowGroupMap != null && !conduitGateFlowGroupMap.isEmpty()) {
				final int groupMapSize = conduitGateFlowGroupMap.size();
				logger.log(Level.INFO, "Found {0} groups to compute.", groupMapSize);
				Set<Map.Entry<IFlowGroup, LocationGroup>> entrySet = conduitGateFlowGroupMap.entrySet();
				if (entrySet != null && !entrySet.isEmpty()) {
					FlowGroupCalc flowGroupCalc = new FlowGroupCalc();
				
					int count = 0;
					for (Map.Entry<IFlowGroup, LocationGroup> entry : entrySet)
                    {
						IFlowGroup flowGroup = entry.getKey();
						LocationGroup locationGroup = entry.getValue();
                        
                        if (isProjectTotalFlowGroup(flowGroup))
                        {
                            //Special case for Project, as we need to use the Release Overrides
                            computeReleaseOverrides(flowGroup, startTime, endTime, options, atTimeSeriesManager);
                            
                            logger.log(Level.INFO, "Compute {0}/{1} Group:{2} completed normally.",
												new Object[]{count, groupMapSize, flowGroup.getId()});
                            continue;
                        }
                        
						count ++;
						try {
						//				FlowGroupTimeSeries newFlowGroupTimeSeries = flowGroup.newFlowGroupTimeSeries(parameterType,
							//			interval, duration, version,
							//			intervalOffsetSeconds, startCal.getTime(), null);
							logger.log(Level.INFO, "Computing {0}/{1} Group:{2}", new Object[]{count, groupMapSize, flowGroup.getId()});
							Map<FlowGroupTimeSeries, ITimeSeriesComputationResult> calcTimeSeries =
								flowGroupCalc.calcTimeSeries(getManagerId(), flowGroup,  startTime, endTime, utcZone, options);
							if(calcTimeSeries == null){
								// I think that this means there was an error...
								logger.log(Level.INFO, "Compute {0}/{1} Group:{2} returned null.", new Object[]{count, groupMapSize, flowGroup.getId()});
							} else {
								
								if (isCompleteFailure(calcTimeSeries)) {
									logger.log(Level.INFO, "Compute {0}/{1} Group:{2} Failed. ", 
											new Object[]{count, groupMapSize, flowGroup.getId()});
								} else {
									DbCommitFlowGroupCalc.storeToTimeSeriesManager(calcTimeSeries, atTimeSeriesManager);

									int failureCount = failureCount(calcTimeSeries);
									if (0 < failureCount) {
										logger.log(Level.INFO, "Compute {0}/{1} Group:{2} completed with {3} Errors.",
												new Object[]{count, groupMapSize, flowGroup.getId(), failureCount});
									} else {
										logger.log(Level.INFO, "Compute {0}/{1} Group:{2} completed normally.",
												new Object[]{count, groupMapSize, flowGroup.getId()});
									}
								}
							}
							//ITimeSeriesComputationResult computationResult = calcTimeSeries.get(newFlowGroupTimeSeries);

						} catch ( FlowGroupComputationException | DataSetException ex) {
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
		Metrics metrics = MetricsServiceProvider.createMetrics(this.getClass().getSimpleName(), "computeFlowGroup");
		OptionalParams options = new OptionalParams(metrics);

		LocationTemplate projectLocRef = new LocationTemplate(officeId, locationId);
		try {
			Map<IFlowGroup, LocationGroup> conduitGateFlowGroupMap = flowGroupManager.retrieveFlowGroups(projectLocRef,
				null, CacheUsage.NORMAL);

			Set<Map.Entry<IFlowGroup, LocationGroup>> entrySet = conduitGateFlowGroupMap.entrySet();

			FlowGroupCalc flowGroupCalc = new FlowGroupCalc();
            
			for (Map.Entry<IFlowGroup, LocationGroup> entry : entrySet) {
				IFlowGroup flowGroup = entry.getKey();
				LocationGroup locationGroup = entry.getValue();

				computeGroup(flowGroup, groupId, flowGroupCalc, startTime, endTime, options, atTimeSeriesManager);
			}

		} catch (DbConnectionException | DbIoException ex) {
			Logger.getLogger(ScriptableGateFlowImpl.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	public void computeGroup(IFlowGroup flowGroup, String groupId, FlowGroupCalc flowGroupCalc, long startTime, long endTime,
		OptionalParams options, AtTimeSeriesManager atTimeSeriesManager)
	{
		if (flowGroup.getId().equals(groupId)) {

			try {
				final TimeZone utcZone = null;
                
                if (isProjectTotalFlowGroup(flowGroup))
                {
                    //Computing the overrides also stores them in the TS Manager.
                    computeReleaseOverrides(flowGroup, startTime, endTime, options, atTimeSeriesManager);
                    logger.log(Level.INFO, "Compute Group:{0} completed normally.", new Object[]{flowGroup.getId()});
                    return;
                }
                
				Map<FlowGroupTimeSeries, ITimeSeriesComputationResult> calcTimeSeries
						= flowGroupCalc.calcTimeSeries(getManagerId(), flowGroup, startTime, endTime, utcZone, options);

				if (calcTimeSeries == null) {				
					logger.log(Level.INFO, " Group:{0} returned null.", new Object[]{flowGroup.getId()});
				} else if (isCompleteFailure(calcTimeSeries)) {
					logger.log(Level.INFO, "Compute Group:{0} Failed. ", new Object[]{flowGroup.getId()});
				} else {
					DbCommitFlowGroupCalc.storeToTimeSeriesManager(calcTimeSeries, atTimeSeriesManager);

					int failureCount = failureCount(calcTimeSeries);
					if (0 < failureCount) {
						logger.log(Level.INFO, "Compute Group:{0} completed with {1} Errors.",
								new Object[]{flowGroup.getId(), failureCount});
					} else {
						logger.log(Level.INFO, "Compute Group:{0} completed normally.",
								new Object[]{flowGroup.getId()});
					}
				}

			} catch (FlowGroupComputationException | DataSetException ex) {
				String message = String.format("Scripted compute for group:%s encountered an exception.", new Object[]{flowGroup.getId()});
				Logger.getLogger(ScriptableGateFlowImpl.class.getName()).log(Level.WARNING, message, ex);
			}
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

	private boolean isCompleteFailure(Map<FlowGroupTimeSeries, ITimeSeriesComputationResult> calcTimeSeries) {
		boolean isEpicFail = true;
		if(calcTimeSeries != null && !calcTimeSeries.isEmpty()){
			for (Map.Entry<FlowGroupTimeSeries, ITimeSeriesComputationResult> entry : calcTimeSeries.entrySet()) {
				FlowGroupTimeSeries key = entry.getKey();
				ITimeSeriesComputationResult value = entry.getValue();

				if (value instanceof TimeSeriesComputationData) {
					isEpicFail = false;
					break;
				}
			}
		}
		
		return isEpicFail;
	}

	private int failureCount(Map<FlowGroupTimeSeries, ITimeSeriesComputationResult> calcTimeSeries) {
		int failCount = 0; 
		if(calcTimeSeries != null && !calcTimeSeries.isEmpty()){
			for (Map.Entry<FlowGroupTimeSeries, ITimeSeriesComputationResult> entry : calcTimeSeries.entrySet()) {
				FlowGroupTimeSeries key = entry.getKey();
				ITimeSeriesComputationResult value = entry.getValue();

				if (!(value instanceof TimeSeriesComputationData) || (value instanceof Exception)) {
					failCount++;
				} 
			}
		}
		
		return failCount;
	}
    
    private boolean isProjectTotalFlowGroup(IFlowGroup flowGroup)
    {
        return flowGroup.getIdSuffix().equals(DefaultFlowGroup.PROJECT.getGroupSuffix());
    }

    private void computeReleaseOverrides(IFlowGroup flowGroup, long startTime, long endTime, OptionalParams params, AtTimeSeriesManager atMan)
    {
        List<FlowGroupTimeSeries> outputTs = flowGroup.getOutputTimeSeriesList();
        Date startDate = new Date(startTime);
        Date endDate = new Date(endTime);
        OptionalParams options = new OptionalParams(params.getMetrics().createMetrics("computeReleaseOverrides"));
        
        AtProjectDescriptor projDesc = new AtProjectDescriptor();
        projDesc.setProjectLocationRef(flowGroup.getLocationRef());
        
        FlowGroupOverrideDataAdapterBase adapter = new FlowGroupOverrideDataAdapterBase(projDesc, getManagerId());
        for (FlowGroupTimeSeries fgts : outputTs)
        {
            try
            {
                int intervalOffsetInSeconds = atMan.retrieveUtcIntervalOffset(fgts.getDescriptionTx());
                
                //default to 0 if it's not valid.
                if (intervalOffsetInSeconds == UtcOffsetConst.NO_UTC_OFFSET || intervalOffsetInSeconds == UtcOffsetConst.UNDEFINED_UTC_OFFSET)
                {
                    intervalOffsetInSeconds = 0;
                }
                
                DataSetTx dstx = adapter.getMergedTimeSeries(flowGroup, new HashSet<IFlowGroup>(), fgts.getInterval(), fgts.getParameterTypeString(), startDate, endDate, null, options, intervalOffsetInSeconds);
            }
            catch (DbConnectionException | DbIoException | DataSetException ex)
            {
                Logger.getLogger(ScriptableGateFlowImpl.class.getName()).log(Level.INFO, "Unable to compute " + fgts.toString() + ", see error log. ", ex);
            }
        }
    }
}
