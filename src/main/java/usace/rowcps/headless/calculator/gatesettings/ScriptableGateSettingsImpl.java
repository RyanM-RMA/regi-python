package usace.rowcps.headless.calculator.gatesettings;

import hec.data.DataSetException;
import hec.data.DataSetIllegalArgumentException;
import hec.data.Duration;
import hec.data.ITimeSeriesDescription;
import hec.data.Interval;
import hec.data.Parameter;
import hec.data.ParameterType;
import hec.data.Units;
import hec.data.UtcOffsetConst;
import hec.data.location.AssignedLocation;
import hec.data.location.Location;
import hec.data.location.LocationCategoryRef;
import hec.data.location.LocationGroup;
import hec.data.location.LocationGroupRef;
import hec.data.location.LocationGroupSet;
import hec.data.location.LocationTemplate;
import hec.data.rating.IRatingSpecification;
import hec.data.rating.JDomRatingSpecification;
import hec.data.tx.DataSetTx;
import hec.data.tx.DataSetTxIllegalArgumentException;
import hec.data.tx.DataSetTxTemplate;
import hec.data.tx.DescriptionTx;
import hec.db.DbConnectionException;
import hec.db.DbException;
import hec.db.DbIoException;
import hec.hecmath.HecMath;
import hec.hecmath.HecMathException;
import hec.hecmath.TimeSeriesMath;
import hec.io.TimeSeriesContainer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import rma.util.RMAConst;
import usace.rowcps.computation.common.IEventThreadExceptionProcessor;
import usace.rowcps.computation.common.IThreadedBlockRetriever;
import usace.rowcps.computation.common.grouping.IControlledOutlet;
import usace.rowcps.computation.common.grouping.IControlledOutletGroup;
import usace.rowcps.computation.common.grouping.IControlledOutletGroupContainer;
import usace.rowcps.computation.TimeSeriesIds;
import usace.rowcps.computation.gatesettings.common.AggregateGateOpeningEntry;
import usace.rowcps.computation.gatesettings.common.DischargeComputationRecord;
import usace.rowcps.computation.gatesettings.common.GateCache;
import usace.rowcps.computation.gatesettings.common.GateMergeException;
import usace.rowcps.computation.gatesettings.common.GateOpeningEntry;
import usace.rowcps.computation.gatesettings.common.GateSettingsBlock;
import usace.rowcps.computation.gatesettings.common.OutletGroup;
import usace.rowcps.data.CacheInitializationException;
import usace.rowcps.headless.calculator.AbstractScriptableCalc;
import usace.rowcps.headless.interfaces.ScriptableCalc;
import usace.rowcps.data.DataObjectException;
import usace.rowcps.data.InputOutput;
import usace.rowcps.data.association.IAssociationCatalog;
import usace.rowcps.data.association.IAssociationProvider;
import usace.rowcps.data.association.ITimeSeriesAssociation;
import usace.rowcps.data.outlet.IOutlet;
import usace.rowcps.data.physicalstructure.IPhysicalStructure;
import usace.rowcps.data.project.AtProjectDescriptor;
import usace.rowcps.data.project.IProject;
import usace.rowcps.headless.calculator.inflow.AbstractThreadedBlockRetriever;
import usace.rowcps.regi.model.AtAssociationManager;
import usace.rowcps.regi.model.AtLocationGroupManager;
import usace.rowcps.regi.model.AtOutletManager;
import usace.rowcps.regi.model.AtProjectManager;
import usace.rowcps.regi.model.AtRatingManager;
import usace.rowcps.regi.model.AtTimeSeriesManager;
import usace.rowcps.regi.model.CacheUsage;
import usace.rowcps.regi.model.ManagerId;
import usace.rowcps.regi.model.RegiDomain;

public class ScriptableGateSettingsImpl extends AbstractScriptableCalc implements ScriptableCalc, ScriptableGateSettings
{

	private static final Logger logger = Logger.getLogger(ScriptableGateSettings.class.getName());

	public ScriptableGateSettingsImpl(RegiDomain regiDomain, ManagerId managerId)
	{
		super(regiDomain, managerId);
	}

	@Override
	public void createGateSettings(String officeId, String locationStr, Date startDate, Date end) throws Exception
	{
		LocationTemplate locRef = new LocationTemplate(officeId, locationStr);
		GateCache gc = getCache(locRef, startDate, end);

		IControlledOutletGroupContainer outletGroupContainer = gc.getOutletGroupContainer();
		if (outletGroupContainer != null) {
			List<IControlledOutletGroup> outletGroups = outletGroupContainer.getOutletGroups();
			if (outletGroups != null) {
				for (IControlledOutletGroup outletGroup : outletGroups) {
					createGateSettingsGroup(gc, locRef, startDate, end, outletGroup);
				}
			}
		}

		gc.saveData();
	}

	@Override
	public void createGateSettingsOutlet(String officeId, String locationStr, Date startDate, Date end, String outletId) throws
		DbConnectionException, DbIoException, CacheInitializationException, DbException, DataSetException,
		DataSetIllegalArgumentException, HecMathException, Exception
	{
		LocationTemplate locRef = new LocationTemplate(officeId, locationStr);
		GateCache gc = getCache(locRef, startDate, end);

		IControlledOutlet iControlledOutlet = getIControlledOutlet(gc, outletId);
		if (iControlledOutlet != null) {

			ITimeSeriesAssociation association = getInputAssociation(locRef);
			Map<String, IOutlet> outletsBySubMap = getOutletsBySubMap(locRef);
			Map<String, TimeSeriesIds> tsIdsBySubMap = initTsIdsBySubLocation(outletsBySubMap.values(), association);
			LocationGroupSet lgs = getLocationGroupSet(locRef);
			if (lgs != null) {
				for (Map.Entry<String, IOutlet> entry : outletsBySubMap.entrySet()) {
					String key = entry.getKey();
					IOutlet value = entry.getValue();
					LocationGroupRef locationGroupRef = value.getRatingGroupRef();

					LocationGroup locationGroup = lgs.getLocationGroup(locationGroupRef);
					List<String> controlParameters = getControlParameters(locationGroup, locRef);

					TimeSeriesIds tsIds = tsIdsBySubMap.get(key);
					updateParameters(tsIds, controlParameters);
				}
			}

			createGateSettingsOutlet(gc, locRef, startDate, end, iControlledOutlet, tsIdsBySubMap);
		}

		gc.saveData();

	}

	@Override
	public void createGateSettingsOutletFromTs(String officeId, String locationStr, Date startDate, Date end, String outletId, String tsId)
		throws
		DbConnectionException, DbIoException, CacheInitializationException, DbException, DataSetException,
		DataSetIllegalArgumentException, HecMathException, Exception
	{
		LocationTemplate locRef = new LocationTemplate(officeId, locationStr);
		GateCache gc = getCache(locRef, startDate, end);

		IControlledOutlet iControlledOutlet = getIControlledOutlet(gc, outletId);
		if (iControlledOutlet != null) {
			createGateSettingsOutlet(gc, locRef, startDate, end, iControlledOutlet, tsId);
		}

		gc.saveData();
	}

	public IControlledOutlet getIControlledOutlet(GateCache gc, String outletId)
	{
		IControlledOutlet retval = null;
		IControlledOutletGroupContainer ogc = gc.getOutletGroupContainer();
		if (ogc != null) {
			Map<String, IControlledOutlet> outletMap = ogc.getOutletMap();
			if (outletMap != null) {
				retval = outletMap.get(outletId);
			}
		}
		return retval;
	}

	@Override
	public void createGateSettingsGroup(String officeId, String locationStr, Date startDate, Date end, String groupId) throws Exception
	{
		LocationTemplate locRef = new LocationTemplate(officeId, locationStr);
		GateCache gc = getCache(locRef, startDate, end);

		IControlledOutletGroupContainer ogc = gc.getOutletGroupContainer();
		if (ogc != null) {
			IControlledOutletGroup outletGroup = ogc.getOutletGroup(groupId);
			if (outletGroup != null) {
				createGateSettingsGroup(gc, locRef, startDate, end, outletGroup);
			}
		}

		gc.saveData();
	}

//	GateCache GateCache = getCache(locRef, startDate);
	public GateCache getCache(LocationTemplate locRef, Date startDate, Date endDate) throws DbConnectionException, DbIoException,
		CacheInitializationException
	{
		RegiDomain domain = getRegiDomain();
		AtProjectManager atProjectManager = domain.getAtProjectManager(getManagerId());
		AtProjectDescriptor projectDescriptor = atProjectManager.getProjectDescriptor(locRef, CacheUsage.NORMAL);
		IEventThreadExceptionProcessor eventThreadExceptionProcessor = null;

		// This seems like a really big hack.  Is this really needed?
		final CountDownLatch latch = new CountDownLatch(2);
		IThreadedBlockRetriever completionCallbackTarget = new AbstractThreadedBlockRetriever()
		{

			@Override
			public void asyncHeadCacheFetchCompleted()
			{
				logger.info("asyncHeadCacheFetchCompleted");
				latch.countDown();
			}

			@Override
			public void asyncTailCacheFetchCompleted()
			{
				logger.info("asyncTailCacheFetchCompleted");
				latch.countDown();
			}

		};

		Set<Date> modifiedDatesForCachedSettings = null;
		GateCache gateCache = new GateCache(getManagerId(), projectDescriptor, 35, completionCallbackTarget, modifiedDatesForCachedSettings);
		gateCache.setDisplayUnitSystem(hec.data.Units.SI_ID);  // does this matter?
		gateCache.initCache(startDate);

		logger.info("Waiting for GateCache to initialize.");
		boolean completedWithoutTimeout = false;
		try {
			// This needs more thought.  Peter thinks db timesout at 10 minutes.
			// Needs to be a value higher than any user would be willing to wait.
			completedWithoutTimeout = latch.await(11, TimeUnit.MINUTES); // This one goes to 11...
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			Logger.getLogger(ScriptableGateSettingsImpl.class.getName()).log(Level.SEVERE, null, ex);
			completedWithoutTimeout = false;
		}

		if (completedWithoutTimeout) {
			gateCache.appendDataToHeadOfCache(35, -1, startDate);
			gateCache.appendDataToTailOfCache(35, -1, endDate);
			logger.info("GateCache is initialized.");
		} else {
			// error
			gateCache = null;
			logger.info("GateCache failed to initialize.");
		}

		return gateCache;
	}

	public ITimeSeriesDescription getFirstTimeSeriesDescription(LocationTemplate locRef) throws DbConnectionException, DbIoException,
		DataObjectException
	{
		ITimeSeriesDescription tsDescription = null;

		RegiDomain regi = getRegiDomain();
		ManagerId manId = getManagerId();

		AtProjectManager atProjectManager = regi.getAtProjectManager(managerId);

//		IProject locProject = atProjectManager.getIProject(locRef, CacheUsage.NORMAL);
//		final IAssociationProvider<ITimeSeriesAssociation> tsProvider = locProject.getTimeSeriesAssociationProvider();
//        final IAssociationCatalog<ITimeSeriesAssociation> global = tsProvider.getGlobalOutputAssociations();
//        final IAssociationCatalog<ITimeSeriesAssociation> override = tsProvider.getOverrideOutputAssociations();
		AtAssociationManager<ITimeSeriesAssociation> assMan = regi.getAtTimeSeriesAssociationManager(manId);

		/// IProject.TsUsageId.GATESETTINGS_GATE_OPENING
		String assType = ITimeSeriesAssociation.LOCATION_TIME_SERIES_ASSOCIATION;

		// String usageCat = "Regi_*_OUTPUT";
		// String usageCat = Regi_gate_OUTPUT
		// String usageCat = IProject.TsUsageId.GATESETTINGS_GATE_OPENING.getUsageCategoryId(RegiDomain.getAppRootId(), InputOutput.INPUT);
		String usageCat = IProject.TsUsageId.GATESETTINGS_GATE_OPENING.getUsageCategoryId(RegiDomain.getAppRootId(), InputOutput.OUTPUT);
		String usageMask = IProject.TsUsageId.GATESETTINGS_GATE_OPENING.usage;   // GateSettings_gate_opening

		// AtAssociationManager.retrieveAssociations builds something like usageCat.usageMask.locRef
		// our gate association is like:
		// 	Regi_gate_OUTPUT.GateSettings_gate_opening.WTYT2
		IAssociationCatalog<ITimeSeriesAssociation> catalog = assMan.retrieveAssociations(locRef, assType, usageCat, usageMask);

		List<ITimeSeriesAssociation> tsas = catalog.getList();

		if (tsas != null && !tsas.isEmpty()) {
			ITimeSeriesAssociation first = tsas.iterator().next();

			tsDescription = first.getTimeSeriesId();
		}

		return tsDescription;
	}

	public Set<Long> getTimeOfChanges(ITimeSeriesDescription timeSeriesId, Date startDate, Date end) throws
		DataSetException, HecMathException, DbException
	{
		Set<Long> timeOfChanges = null;
		if (timeSeriesId != null) {

			RegiDomain regi = getRegiDomain();
			AtTimeSeriesManager tsManager = regi.getAtTimeSeriesManager(getManagerId());

			DataSetTx dataSetTX = getDataSetTx(timeSeriesId, startDate, end, tsManager);

			if (dataSetTX != null) {
				timeOfChanges = findChanges(dataSetTX);

			}

		}
		return timeOfChanges;
	}

	public DataSetTx getDataSetTx(ITimeSeriesDescription timeSeriesId, Date startDate, Date end, AtTimeSeriesManager tsManager) throws
		DataSetTxIllegalArgumentException, DbException, DataSetException
	{
		DataSetTx dstx = null;
		//String timeSeriesId2 = timeSeriesId.getTimeSeriesId();
		DescriptionTx dTx = timeSeriesId.getLookup().lookup(DescriptionTx.class);

		dstx = getDataSetTx(dTx, startDate, end, tsManager);

		return dstx;
	}

	public DataSetTx getDataSetTx(DescriptionTx dTx, Date startDate, Date end, AtTimeSeriesManager tsManager) throws DbException,
		DataSetException, DataSetTxIllegalArgumentException
	{
		DataSetTx dstx;
		String timeSeriesId1 = dTx.getTimeSeriesId();
		Units units = dTx.getParameter().getUnits();
		//Units units = new Units();
		DataSetTxTemplate dataSetTxTemplate = new DataSetTxTemplate(dTx, startDate.getTime(), end.getTime(), units);
		dstx = tsManager.retrieveDataSetTx(dataSetTxTemplate, CacheUsage.NORMAL);
		return dstx;
	}

	public NavigableSet<Long> findChanges(DataSetTx dataSetTX) throws HecMathException
	{
		NavigableSet<Long> set = null;
		if (dataSetTX != null) {
			set = new TreeSet<>();
			TimeSeriesContainer tsc = dataSetTX.getTimeSeriesContainer();
			TimeSeriesMath tsm = new TimeSeriesMath(tsc);

			double[] values = dataSetTX.getValuesWithQualityFlagsApplied();  // do we want this?
			long[] times = dataSetTX.getTimes();

			double lastValid = HecMath.UNDEFINED;

			// add the times of the internal changes.
			for (int i = 0; i < values.length; i++) {
				if (tsm.isValid(i)) {
					if (lastValid != values[i]) {
						lastValid = values[i];
						set.add(times[i]);
					}
				}
			}

			// add the bookends
			set.add(times[0]);
			set.add(times[times.length - 1]);
		}

		return set;
	}

	public void createGateSettingsGroup(GateCache gc, LocationTemplate locRef, Date startDate, Date end, IControlledOutletGroup outletGroup)
		throws DataSetException, DbException, HecMathException
	{
		List<IControlledOutlet> outlets = outletGroup.getOutlets();

		if (outlets != null && !outlets.isEmpty()) {
			ITimeSeriesAssociation association = getInputAssociation(locRef);
			Map<String, IOutlet> outletsBySubMap = getOutletsBySubMap(locRef);

			Map<String, TimeSeriesIds> tsIdsBySubMap = initTsIdsBySubLocation(outletsBySubMap.values(), association);

			LocationGroupSet lgs = getLocationGroupSet(locRef);
			if (lgs != null) {
				for (Map.Entry<String, IOutlet> entry : outletsBySubMap.entrySet()) {
					String key = entry.getKey();
					IOutlet value = entry.getValue();
					LocationGroupRef locationGroupRef = value.getRatingGroupRef();

					LocationGroup locationGroup = lgs.getLocationGroup(locationGroupRef);
					List<String> controlParameters = getControlParameters(locationGroup, locRef);

					TimeSeriesIds tsIds = tsIdsBySubMap.get(key);
					updateParameters(tsIds, controlParameters);
				}
			}

			for (IControlledOutlet controlledOutlet : outlets) {
				createGateSettingsOutlet(gc, locRef, startDate, end, controlledOutlet, tsIdsBySubMap);
			}
		}
	}

	public static void updateParameters(TimeSeriesIds tsIds, List<String> controlParameters)
	{
		if (tsIds != null && controlParameters != null && !controlParameters.isEmpty()) {
			String firstControlParam = controlParameters.get(0);

			tsIds.setParameter(0, firstControlParam);  // updates parameter from "Opening" to "Opening-Spillway_Gate"

			if (controlParameters.size() > 1) {
				// not exactly sure how this next bit works but this is what OutletPanel does....
				for (int j = 1; j < controlParameters.size(); j++) {
					tsIds.add(tsIds.getId(0), tsIds.getOffset(0));
					tsIds.setParameter(j, controlParameters.get(j));
				}
			}
		}
	}

	private LocationGroupSet getLocationGroupSet(LocationTemplate locRef) throws DbConnectionException, DbIoException
	{
		RegiDomain regiDomain1 = getRegiDomain();
		AtProjectManager atProjectManager = regiDomain1.getAtProjectManager(getManagerId());
		AtProjectDescriptor currentDescriptor = atProjectManager.getProjectDescriptor(locRef, CacheUsage.NORMAL);
		LocationTemplate projectLocationRef = currentDescriptor.getProjectLocationRef();
		AtRatingManager atRatingManager = regiDomain1.getAtRatingManager(getManagerId());
		LocationCategoryRef ratingGroupCatRef = atRatingManager.getCategoryRef();
		String officeId = projectLocationRef.getOfficeId();

		AtLocationGroupManager atLocationGroupManager = regiDomain1.getAtLocationGroupManager(getManagerId());
		LocationGroupSet ratingGroups = atLocationGroupManager.retrieveLocationGroups(ratingGroupCatRef, officeId,
			projectLocationRef, null,
			CacheUsage.NORMAL);
		return ratingGroups;
	}

	/**
	 * Based on the method in OutletPanel..
	 *
	 * @param rating
	 * @param template
	 * @return
	 */
	private List<String> getControlParameters(LocationGroup rating, LocationTemplate template)
	{

		List<String> retval = new ArrayList<>();
		if (rating != null) {
			for (AssignedLocation assignedLocation : rating.getAssignedLocations()) {
				if (assignedLocation.getAssociatedLocRef() != null && assignedLocation.getAssociatedLocRef().equals(template)) {
					retval.add(assignedLocation.getAliasId());
				}
			}

			if (retval.isEmpty()) {
				try {
					String ratingSpecId = rating.getSharedLocAliasId();
					RegiDomain currentProject = (RegiDomain) RegiDomain.getCurrentProject();
					IRatingSpecification ratingSpecification = new JDomRatingSpecification(currentProject.getUserOfficeId(), ratingSpecId);

					AtRatingManager ratingManager = currentProject.getAtRatingManager(getManagerId());
					Parameter controlParameter = ratingManager.getOutletOpeningParameter(ratingSpecification);
					if (controlParameter != null) {
						retval.add(controlParameter.toString());
					}
				} catch (DataSetException ex) {
					logger.log(Level.INFO, "unable to get control parameter for rating group {0}", rating.getDisplayName());
				}
			}
		}

		return retval;
	}

	public NavigableSet<Date> getDatesSubset(GateCache gc, Date start, Date end)
	{
		NavigableSet<Date> retval = new TreeSet<>();

		if (gc != null) {
			Date[] gateSettingKeys = gc.getGateSettingKeys();
			if (gateSettingKeys != null && gateSettingKeys.length > 0) {
				retval.addAll(Arrays.asList(gateSettingKeys));
				retval = retval.subSet(start, true, end, true);
			}
		}

		return retval;
	}

	private void createGateSettingsOutlet(GateCache gc, LocationTemplate locRef, Date startDate, Date end,
		IControlledOutlet iControlledOutlet, Map<String, TimeSeriesIds> tsIdsBySubMap) throws DbConnectionException, DbIoException,
		DataSetIllegalArgumentException, DbException,
		DataSetException, HecMathException
	{
		if (iControlledOutlet != null) {

//				//	ITimeSeriesDescription timeSeriesId = association.getTimeSeriesId();
//		List<IOutlet> outlets = getIOutlets(regi, locProject);
//
//		Map<String, IOutlet> outletsBySubMap = new HashMap<>();
//		for (IOutlet outlet : outlets) {
//			outletsBySubMap.put(outlet.getLocation().getSubLocationId(), outlet);
//		}
//			String outletName = iControlledOutlet.getOutletName();   // this is like TG1 , not like WTYT2-TG1
			String tsIdStr = getFirstTimeSeriesDescription(locRef, iControlledOutlet.toString(), tsIdsBySubMap);
			createGateSettingsOutlet(gc, locRef, startDate, end, iControlledOutlet, tsIdStr);
		}
	}

	public void createGateSettingsOutlet(GateCache gc, LocationTemplate locRef, Date startDate, Date end,
		IControlledOutlet iControlledOutlet, String tsIdStr) throws DataSetException, DbException, HecMathException
	{
		AtTimeSeriesManager tsManager = getRegiDomain().getAtTimeSeriesManager(getManagerId());

		logger.log(Level.INFO, "Comparing Gate settings at:{0} to timeseries:{1}", new Object[]{iControlledOutlet.getOutletName(), tsIdStr});

		if (tsIdStr != null) {
			DescriptionTx dtx = new DescriptionTx(locRef.getOfficeId(), tsIdStr);
			DataSetTx dataSetTx = getDataSetTx(dtx, startDate, end, tsManager);

			NavigableSet<Long> timeOfChanges = findChanges(dataSetTx);
			if (timeOfChanges != null && !timeOfChanges.isEmpty()) {

				NavigableMap<Date, Double> tsMap = dataSetTx.getDateValueNavigableMap();
				NavigableMap<Date, GateOpeningEntry> dateEntryMap = findEntriesForOutlet(iControlledOutlet, gc, startDate, end);
//				NavigableSet<Date> cacheDates = getDatesSubset(gc, startDate, end);

				TreeSet<Date> datesOfModifications = new TreeSet<>();
				// Coming from the back lets us ignore the gate-entries we are adding.
				// Also we only care about the changes in the timeseries.
				for (Iterator<Long> iterator = timeOfChanges.descendingIterator(); iterator.hasNext();) {
					Long next = iterator.next();
					if (next != null) {
						Date tsDate = new Date(next);

						Double tsValue = tsMap.get(tsDate);
// Debugging...
//					if (iControlledOutlet instanceof Outlet) {
//					Units tsUnits = dataSetTx.getUnits();
//						Outlet outlet = (Outlet) iControlledOutlet;
//						String siUnits = outlet.getSiUnits();
//						logger.finer("outlet " + outlet.getOutletName() + " is in units:" + siUnits);
//					}
						if (tsValue != null && RMAConst.isValidValue(tsValue)) {

							Map.Entry<Date, GateOpeningEntry> floorEntry = getValidFloorEntry(dateEntryMap, tsDate);
							Double cacheValueInEffectAtDate = getValueFromEntry(floorEntry);

							if (!Objects.equals(cacheValueInEffectAtDate, tsValue)) {
								try {
									// difference detected
									// do we immediately make the change or do we build objects and make all the changes later? Immediate

									GateSettingsBlock gateSettingBlock = gc.getGateSetting(tsDate);
									if (gateSettingBlock == null) {
										ArrayList<AggregateGateOpeningEntry> aggregateOpenings = new ArrayList<AggregateGateOpeningEntry>();
										gateSettingBlock = new GateSettingsBlock(gc.getOutletGroupContainer().getOutletGroups(), aggregateOpenings);
										String dischargeCode = DischargeComputationRecord.DischargeComputationCode.EstimatedByUser.dbEquivalent();
										gateSettingBlock.setDischargeComputationCode(dischargeCode);
										gateSettingBlock.setReleaseReasonCode("O");
										gateSettingBlock.setChangeNotes("Regi Headless ");
										gateSettingBlock.setModified(true);
										gc.putGateSetting(tsDate, gateSettingBlock);
									}

									

									boolean modifyRetval = gc.modifyGateOpeningBlock(tsDate, iControlledOutlet, tsValue);
									datesOfModifications.add(tsDate);
								} catch (GateMergeException ex) {
									Logger.getLogger(ScriptableGateSettingsImpl.class.getName()).log(Level.WARNING, null, ex);
								}
							}
						}
					}
				}

				logger.log(Level.INFO, "Modifications were made to the gate settings at {0} for the following {1} dates:{2}",
					new Object[]{iControlledOutlet.getOutletName(), datesOfModifications.size(), datesOfModifications});


			}
		}
	}

	public Map.Entry<Date, GateOpeningEntry> getValidFloorEntry(NavigableMap<Date, GateOpeningEntry> dateEntryMap, Date tsDate)
	{
		Map.Entry<Date, GateOpeningEntry> floorEntry = null;

		Double value = null;
		Date nextKey = tsDate;
		while (nextKey != null && (value == null || !RMAConst.isValidValue(value))) {
			floorEntry = dateEntryMap.floorEntry(nextKey);
			value = getValueFromEntry(floorEntry);
			nextKey = dateEntryMap.lowerKey(nextKey);
		}

		return floorEntry;
	}

	public Double getValueFromEntry(Map.Entry<Date, GateOpeningEntry> floorEntry)
	{
		Double retval = null;
		if (floorEntry != null) {
			GateOpeningEntry entry = floorEntry.getValue();
			// Date entryDate = floorEntry.getKey();
			AggregateGateOpeningEntry agoeHolding = entry.getParent();
			if (agoeHolding != null) {
				retval = agoeHolding.getGateOpeningCommon();
			}
		}
		return retval;
	}

	public NavigableMap<Date, GateOpeningEntry> findEntriesForOutlet(IControlledOutlet iControlledOutlet, GateCache gc, Date startDate,
		Date end)
	{
		NavigableMap<Date, GateOpeningEntry> dateEntryMap = null;
		if (iControlledOutlet != null) {
			Date[] gateSettingKeys = gc.getGateSettingKeys();

			TreeSet<Date> dateset = new TreeSet<>();
			if (gateSettingKeys != null && gateSettingKeys.length > 0) {
				dateset.addAll(Arrays.asList(gateSettingKeys));
			}

			Date startAt = dateset.floor(startDate);
			// I think I need the date before the startDate too so that I know what it changed from at startDate?
			if (startAt == null) {
				startAt = startDate;
			}

			SortedSet<Date> subSet = dateset.subSet(startAt, end);

			// this is the way its structured in the cache
			NavigableMap<Date, Map<IControlledOutletGroup, Map<IControlledOutlet, GateOpeningEntry>>> dateGrpOutletEntryMap
				= buildDateGrpOutletEntryMap(subSet, gc);

			// Pretty sure it works better for me if it looks like this
			Map<IControlledOutletGroup, Map<IControlledOutlet, NavigableMap<Date, GateOpeningEntry>>> grpOutletDateMap
				= reorganize(dateGrpOutletEntryMap);

			IControlledOutletGroup group = iControlledOutlet.getParent();
			Map<IControlledOutlet, NavigableMap<Date, GateOpeningEntry>> outletDateEntryMap = null;
			if (grpOutletDateMap != null) {
				outletDateEntryMap = grpOutletDateMap.get(group);
			}
			if (outletDateEntryMap != null) {
				dateEntryMap = outletDateEntryMap.get(iControlledOutlet);
			}
		}
		return dateEntryMap;
	}

	public NavigableMap<Date, Map<IControlledOutletGroup, Map<IControlledOutlet, GateOpeningEntry>>> buildDateGrpOutletEntryMap(
		SortedSet<Date> subSet, GateCache gc)
	{
		NavigableMap<Date, Map<IControlledOutletGroup, Map<IControlledOutlet, GateOpeningEntry>>> dateGrpOutletEntryMap = new TreeMap<>();
		if (subSet != null && !subSet.isEmpty()) {
			for (Date date : subSet) {
				GateSettingsBlock gsb = gc.getGateSetting(date);

				if (gsb != null) {

					List<AggregateGateOpeningEntry> entryList = gsb.getReadOnlyAggregateOpenings();
					if (entryList != null && !entryList.isEmpty()) {
						Map<IControlledOutletGroup, Map<IControlledOutlet, GateOpeningEntry>> map = new HashMap<>();
						dateGrpOutletEntryMap.put(date, map);
						for (AggregateGateOpeningEntry entry : entryList) {
							if (entry != null) {
								List<GateOpeningEntry> gateSettings = entry.getReadOnlyGateSettings();

								if (gateSettings != null && !gateSettings.isEmpty()) {
									IControlledOutletGroup controlledOutletGroup = entry.getOutletGroup();
									if (controlledOutletGroup instanceof OutletGroup) {
										OutletGroup outletGroup = (OutletGroup) controlledOutletGroup;
//									try {
//										String openingsUnitsForGroupSI = outletGroup.getOpeningsUnitsForGroup(hec.data.Units.SI_ID);
//									} catch (Exception ex) {
//										Logger.getLogger(ScriptableGateSettingsImpl.class.getName()).log(Level.SEVERE, null, ex);
//									}

									}

									Map<IControlledOutlet, GateOpeningEntry> entries = map.get(controlledOutletGroup);
									if (entries == null) {
										entries = new HashMap<>();
										map.put(controlledOutletGroup, entries);
									}

									for (GateOpeningEntry setting : gateSettings) {
										if (setting != null) {
											entries.put(setting.getOutlet(), setting);
										}
									}
								}
							}
						}
					}
				}
			}
		}
		return dateGrpOutletEntryMap;
	}

	private Map<IControlledOutletGroup, Map<IControlledOutlet, NavigableMap<Date, GateOpeningEntry>>> reorganize(
		NavigableMap<Date, Map<IControlledOutletGroup, Map<IControlledOutlet, GateOpeningEntry>>> dateGrpOutletEntryMap)
	{
		Map<IControlledOutletGroup, Map<IControlledOutlet, NavigableMap<Date, GateOpeningEntry>>> retval = null;

		if (dateGrpOutletEntryMap != null) {
			retval = new HashMap<>();

			for (Map.Entry<Date, Map<IControlledOutletGroup, Map<IControlledOutlet, GateOpeningEntry>>> entry : dateGrpOutletEntryMap.
				entrySet()) {
				Date key = entry.getKey();
				Map<IControlledOutletGroup, Map<IControlledOutlet, GateOpeningEntry>> grpOutletEntryMap = entry.getValue();
				if (grpOutletEntryMap != null && !grpOutletEntryMap.isEmpty()) {
					for (Map.Entry<IControlledOutletGroup, Map<IControlledOutlet, GateOpeningEntry>> entry1 : grpOutletEntryMap.entrySet()) {
						IControlledOutletGroup outletGrp = entry1.getKey();
						Map<IControlledOutlet, GateOpeningEntry> outletEntryMap = entry1.getValue();

						if (outletEntryMap != null && !outletEntryMap.isEmpty()) {
							Map<IControlledOutlet, NavigableMap<Date, GateOpeningEntry>> retOutletDateEntryMap = retval.get(outletGrp);
							if (retOutletDateEntryMap == null) {
								retOutletDateEntryMap = new HashMap<>();
								retval.put(outletGrp, retOutletDateEntryMap);
							}

							for (Map.Entry<IControlledOutlet, GateOpeningEntry> entry2 : outletEntryMap.entrySet()) {
								IControlledOutlet outlet = entry2.getKey();
								GateOpeningEntry openningEntry = entry2.getValue();

								if (openningEntry != null) {

									NavigableMap<Date, GateOpeningEntry> retDateMap = retOutletDateEntryMap.get(outlet);
									if (retDateMap == null) {
										retDateMap = new TreeMap<>();
										retOutletDateEntryMap.put(outlet, retDateMap);
									}

									retDateMap.put(key, openningEntry);
								}
							}
						}
					}
				}
			}
		}

		return retval;

	}

	// not efficient for multiple outlets.  used by test.
	public String getFirstTimeSeriesDescription(LocationTemplate locRef, String outletId) throws DbConnectionException, DbIoException
	{
		ITimeSeriesAssociation association = getInputAssociation(locRef);
		Map<String, IOutlet> outletsBySubMap = getOutletsBySubMap(locRef);
		Map<String, TimeSeriesIds> tsIdsBySubMap = initTsIdsBySubLocation(outletsBySubMap.values(), association);
		return getFirstTimeSeriesDescription(locRef, outletId, tsIdsBySubMap);
	}

// outletId is like TG1
	public String getFirstTimeSeriesDescription(LocationTemplate locRef, String outletId, Map<String, TimeSeriesIds> tsIds) throws
		DbConnectionException, DbIoException
	{
		String retval = null;
		ITimeSeriesDescription tsDescription = null;

		if (tsIds != null) {
			TimeSeriesIds tsIdsForOutlet = tsIds.get(outletId);  // outletId is like TG1
			if (tsIdsForOutlet != null) {
				retval = tsIdsForOutlet.getId(0);
			}
		}
		return retval;
	}

	public ITimeSeriesAssociation getInputAssociation(LocationTemplate locRef) throws DbIoException, DbConnectionException
	{
		RegiDomain regi = getRegiDomain();
		ManagerId manId = getManagerId();

		AtProjectManager atProjectManager = regi.getAtProjectManager(manId);

		IProject locProject = atProjectManager.getIProject(locRef, CacheUsage.NORMAL);
		final IAssociationProvider<ITimeSeriesAssociation> tsProvider = locProject.getTimeSeriesAssociationProvider();
		ITimeSeriesAssociation association = tsProvider.getInputAssociation(IProject.TsUsageId.GATESETTINGS_GATE_OPENING.usage);
		return association;
	}

	private Map<String, IOutlet> getOutletsBySubMap(LocationTemplate locRef) throws DbConnectionException, DbIoException
	{
		Map<String, IOutlet> outletsBySubMap = null;
		RegiDomain regi = getRegiDomain();
		ManagerId manId = getManagerId();

		AtProjectManager atProjectManager = regi.getAtProjectManager(manId);

		IProject locProject = atProjectManager.getIProject(locRef, CacheUsage.NORMAL);

		//	ITimeSeriesDescription timeSeriesId = association.getTimeSeriesId();
		List<IOutlet> outlets = getIOutlets(regi, locProject);

		outletsBySubMap = new HashMap<>();
		for (IOutlet outlet : outlets) {
			if (outlet != null) {
				Location location = outlet.getLocation();
				if (location != null) {
					outletsBySubMap.put(location.getSubLocationId(), outlet);
				}
			}
		}
		return outletsBySubMap;
	}

	public List<IOutlet> getIOutlets(RegiDomain regi, IProject locProject) throws DbConnectionException, DbIoException
	{
		AtOutletManager atOutletManager = regi.getAtOutletManager(managerId);
		List<IOutlet> outlets = new ArrayList<>();
		List<IPhysicalStructure> physicalStructures = atOutletManager.retrieveList(locProject.getLocation().getLocationTemplate(),
			CacheUsage.NORMAL);
		for (IPhysicalStructure physicalStructure : physicalStructures) {
			if (physicalStructure instanceof IOutlet) {
				outlets.add((IOutlet) physicalStructure);
			}

		}
		return outlets;
	}

	private Map<String, TimeSeriesIds> initTsIdsBySubLocation(Collection<IOutlet> outlets, ITimeSeriesAssociation association)
	{
		Map<String, TimeSeriesIds> retval = new HashMap<>();

		if (association != null && association.getTimeSeriesId() != null) {
			//sub in the time series id for each outlet
			ITimeSeriesDescription tsId = association.getTimeSeriesId();

			for (IOutlet outlet : outlets) {
				String str = outlet.toString();  // like WTYT2-TG1
				String subLocationId = outlet.getLocation().getSubLocationId();  // like TG1
				TimeSeriesIds tsIds = new TimeSeriesIds();
				tsIds.add(tsId.getTimeSeriesId(), association.getOffsetSec());
				tsIds.setLocation(str);
				retval.put(subLocationId, tsIds);
			}

		} else {
			String type = ParameterType.getAvailableParameterTypes()[0];
			String interval = Interval.getAvailableIntervals()[0];
			String duration = Duration.getAvailableDurations()[0];
			String version = "MANUAL";
			int offset = UtcOffsetConst.NO_UTC_OFFSET;

			for (IOutlet outlet : outlets) {
				String subLocationId = outlet.getLocation().getSubLocationId();  // like TG1
				String str = outlet.toString();
				TimeSeriesIds tsIds = new TimeSeriesIds();
				tsIds.setLocation(str);
				tsIds.add("", type, interval, duration, version, offset);

				retval.put(subLocationId, tsIds);

			}
		}

		return retval;
	}

}
