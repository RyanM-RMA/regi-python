package usace.rowcps.headless.calculator.gatesettings;

import hec.data.DataSetException;
import hec.data.DataSetIllegalArgumentException;
import hec.data.Duration;
import hec.data.ITimeSeriesDescription;
import hec.data.Interval;
import hec.data.ParameterType;
import hec.data.Units;
import hec.data.UtcOffsetConst;
import hec.data.location.LocationTemplate;
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
import hec.lang.Const;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import usace.rowcps.computation.common.IEventThreadExceptionProcessor;
import usace.rowcps.computation.common.IThreadedBlockRetriever;
import usace.rowcps.computation.common.grouping.IControlledOutlet;
import usace.rowcps.computation.common.grouping.IControlledOutletGroup;
import usace.rowcps.computation.common.grouping.IControlledOutletGroupContainer;
import usace.rowcps.computation.gatesettings.TimeSeriesIds;
import usace.rowcps.computation.gatesettings.common.AggregateGateOpeningEntry;
import usace.rowcps.computation.gatesettings.common.GateCache;
import usace.rowcps.computation.gatesettings.common.GateMergeException;
import usace.rowcps.computation.gatesettings.common.GateOpeningEntry;
import usace.rowcps.computation.gatesettings.common.GateSettingsBlock;
import usace.rowcps.computation.gatesettings.common.Outlet;
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
import usace.rowcps.data.physicalstructure.PhysicalStructure;
import usace.rowcps.data.project.AtProjectDescriptor;
import usace.rowcps.data.project.IProject;
import usace.rowcps.headless.calculator.inflow.AbstractThreadedBlockRetriever;
import usace.rowcps.regi.model.AtAssociationManager;
import usace.rowcps.regi.model.AtOutletManager;
import usace.rowcps.regi.model.AtProjectManager;
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
	}

	@Override
	public void createGateSettingsOutlet(String officeId, String locationStr, Date startDate, Date end, String outletId) throws Exception
	{
		LocationTemplate locRef = new LocationTemplate(officeId, locationStr);
		GateCache gc = getCache(locRef, startDate, end);
		
		IControlledOutlet iControlledOutlet = getIControlledOutlet(gc, outletId);
		if(iControlledOutlet != null){
			createGateSettingsOutlet(gc, locRef, startDate, end, iControlledOutlet);
		}

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
	}

//	GateCache GateCache = getCache(locRef, startDate);
	public GateCache getCache(LocationTemplate locRef, Date startDate, Date endDate) throws DbConnectionException, DbIoException,
		InterruptedException, CacheInitializationException
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
		gateCache.initCache(startDate);

		logger.info("Waiting for GateCache to initialize.");
		// This needs more thought.  Peter thinks db timesout at 10 minutes.
		// Needs to be a value higher than any user would be willing to wait.
		latch.await(11, TimeUnit.MINUTES);  // This one goes to 11...
		logger.info("GateCache is initialized.");

		gateCache.appendDataToHeadOfCache(35, -1, startDate);
		gateCache.appendDataToTailOfCache(35, -1, endDate);

		logger.info("GateCache is initialized.");
		return gateCache;
	}

//	public void createGateSettings(GateCache gc, LocationTemplate locRef, Date startDate, Date end) throws
//		DbConnectionException, DbIoException, DbException, DataSetException, DataObjectException, HecMathException
//	{
//		RegiDomain regi = getRegiDomain();
//		ManagerId manId = getManagerId();
//
//		AtProjectManager atProjectManager = regi.getAtProjectManager(managerId);
//
//		AtOutletManager atOutletManager = regi.getAtOutletManager(managerId);
//		NavigableMap locAssociations = atOutletManager.retrieveLocationAssociations(locRef, CacheUsage.NORMAL);
//
//		List<IOutlet> retrieveList = atOutletManager.retrieveList(locRef, CacheUsage.NORMAL);
//		for (IOutlet iOutlet : retrieveList) {
//			createGateSettingsOutlet(gc, locRef, startDate, end, iOutlet);
//		}
//
////        GateCache commonGateCache = new GateCache(currentDayControl, getManagerId(), projectDescriptor, MAXROWSTORETRIEVE, completionCallbackTarget,
////                modifiedDatesForCachedSettings);
////
////        commonGateCache.initCache();
//	}
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
			createGateSettingsOutlets(gc, locRef, startDate, end, outlets);
		}
	}

	public void createGateSettingsOutlets(GateCache gc, LocationTemplate locRef, Date startDate, Date end, List<IControlledOutlet> outlets)
		throws DbIoException, DbException, DbConnectionException, DataSetException, DataSetIllegalArgumentException, HecMathException
	{
		if (outlets != null && !outlets.isEmpty()) {
			for (IControlledOutlet outlet : outlets) {
				createGateSettingsOutlet(gc, locRef, startDate, end, outlet);
			}
		}
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
		IControlledOutlet iControlledOutlet) throws DbConnectionException, DbIoException, DataSetIllegalArgumentException, DbException,
		DataSetException, HecMathException
	{
		if (iControlledOutlet != null) {
			DataSetTx dataSetTx = getDataSetTx(locRef, iControlledOutlet, startDate, end);

			NavigableSet<Long> timeOfChanges = findChanges(dataSetTx);
			if(timeOfChanges != null && !timeOfChanges.isEmpty()){

				NavigableMap<Date, Double> dateValueNavigableMap = dataSetTx.getDateValueNavigableMap();
				NavigableMap<Date, GateOpeningEntry> dateEntryMap = findEntriesForOutlet(iControlledOutlet, gc, startDate, end);
				NavigableSet<Date> cacheDates = getDatesSubset(gc, startDate, end);

				// Coming from the back lets us ignore the gate-entries we are adding.
				// Also we only care about the changes in the timeseries.
				for (Iterator<Long> iterator = timeOfChanges.descendingIterator(); iterator.hasNext();) {
					Long next = iterator.next();
					if (next != null) {
						Date tsDate = new Date(next);

						Double tsValue = dateValueNavigableMap.get(tsDate);
						Units tsUnits = dataSetTx.getUnits();

						if (iControlledOutlet instanceof Outlet) {
							Outlet outlet = (Outlet) iControlledOutlet;
							String siUnits = outlet.getSiUnits();
							logger.finer("outlet " + outlet.getOutletName() + " is in units:" + siUnits);
						}

						if (tsValue != null && Const.isValid(tsValue)) {
							Date cacheDateBeforeTs = cacheDates.floor(tsDate);
							if (cacheDateBeforeTs != null) {
								Double gateOpeningAtOrBefore = gc.getGateOpening(cacheDateBeforeTs, iControlledOutlet);

								Map.Entry<Date, GateOpeningEntry> floorEntry = dateEntryMap.floorEntry(tsDate);
								if (floorEntry != null) {
									GateOpeningEntry entry = floorEntry.getValue();
//					Date entryDate = floorEntry.getKey();   // is this the same as cacheDateBeforeTs
									AggregateGateOpeningEntry agoeHolding = entry.getParent();
									if (agoeHolding != null) {
										double gateOpeningCommon = agoeHolding.getGateOpeningCommon();  // is this the same as gateOpeniingAtOrBefore?

										if (gateOpeningCommon != tsValue) {
											try {
												// difference detected
												// do we immediately make the change or do we build objects and make all the changes later?
												gc.modifyGateOpeningBlock(tsDate, iControlledOutlet, tsValue);
											} catch (GateMergeException ex) {
												Logger.getLogger(ScriptableGateSettingsImpl.class.getName()).log(Level.WARNING, null, ex);
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}

	public DataSetTx getDataSetTx(LocationTemplate locRef, IControlledOutlet iControlledOutlet, Date startDate, Date end) throws
		DbConnectionException, DataSetException, DbException, DbIoException, DataSetIllegalArgumentException
	{
		DataSetTx dataSetTx = null;
		AtTimeSeriesManager tsManager = getRegiDomain().getAtTimeSeriesManager(getManagerId());
		String tsIdStr = getFirstTimeSeriesDescription(locRef, iControlledOutlet.toString());
		if (tsIdStr != null) {
			DescriptionTx dtx = new DescriptionTx(locRef.getOfficeId(), tsIdStr);
			dataSetTx = getDataSetTx(dtx, startDate, end, tsManager);
		}
		return dataSetTx;
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
			Map<IControlledOutletGroup, Map<IControlledOutlet, NavigableMap<Date, GateOpeningEntry>>> grpOutletDateMap = reorganize(
				dateGrpOutletEntryMap);

			IControlledOutletGroup group = iControlledOutlet.getParent();
			Map<IControlledOutlet, NavigableMap<Date, GateOpeningEntry>> outletDateEntryMap = grpOutletDateMap.get(group);
			dateEntryMap = outletDateEntryMap.get(iControlledOutlet);
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
							List<GateOpeningEntry> gateSettings = entry.getReadOnlyGateSettings();

							if (gateSettings != null && !gateSettings.isEmpty()) {
								IControlledOutletGroup controlledOutletGroup = entry.getOutletGroup();
								if (controlledOutletGroup instanceof OutletGroup) {
									OutletGroup outletGroup = (OutletGroup) controlledOutletGroup;
									try {
										String openingsUnitsForGroupSI = outletGroup.getOpeningsUnitsForGroup(hec.data.Units.SI_ID);
									} catch (Exception ex) {
										Logger.getLogger(ScriptableGateSettingsImpl.class.getName()).log(Level.SEVERE, null, ex);
									}

								}

								Map<IControlledOutlet, GateOpeningEntry> entries = map.get(controlledOutletGroup);
								if (entries == null) {
									entries = new HashMap<>();
									map.put(controlledOutletGroup, entries);
								}

								for (GateOpeningEntry setting : gateSettings) {
									entries.put(setting.getOutlet(), setting);
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

	String getFirstTimeSeriesDescription(LocationTemplate locRef, String outletId) throws DbConnectionException, DbIoException
	{
		String retval = null;
		ITimeSeriesDescription tsDescription = null;

		RegiDomain regi = getRegiDomain();
		ManagerId manId = getManagerId();

		AtProjectManager atProjectManager = regi.getAtProjectManager(managerId);

		IProject locProject = atProjectManager.getIProject(locRef, CacheUsage.NORMAL);
		final IAssociationProvider<ITimeSeriesAssociation> tsProvider = locProject.getTimeSeriesAssociationProvider();
		ITimeSeriesAssociation association = tsProvider.getInputAssociation(IProject.TsUsageId.GATESETTINGS_GATE_OPENING.usage);

		//	ITimeSeriesDescription timeSeriesId = association.getTimeSeriesId();
		List<IOutlet> outlets = getIOutlets(regi, locProject);

		Map<String, TimeSeriesIds> tsIds = initTsIds(outlets, association);
		if (tsIds != null) {
			TimeSeriesIds tsIdsForOutlet = tsIds.get(outletId);
			if (tsIdsForOutlet != null) {
				retval = tsIdsForOutlet.getId(0);
			}
		}
		return retval;
	}

	public List<IOutlet> getIOutlets(RegiDomain regi, IProject locProject) throws DbConnectionException, DbIoException
	{
		AtOutletManager atOutletManager = regi.getAtOutletManager(managerId);
		List<IOutlet> outlets = new ArrayList<>();
		List<PhysicalStructure> physicalStructures = atOutletManager.retrieveList(locProject.getLocation().getLocationTemplate(),
			CacheUsage.NORMAL);
		for (PhysicalStructure physicalStructure : physicalStructures) {
			if (physicalStructure instanceof IOutlet) {
				outlets.add((IOutlet) physicalStructure);
			}

		}
		return outlets;
	}


	private Map<String, TimeSeriesIds> initTsIds(List<IOutlet> outlets, ITimeSeriesAssociation association)
	{
		Map<String, TimeSeriesIds> retval = new HashMap<>();

		if (association != null && association.getTimeSeriesId() != null) {
			//sub in the time series id for each outlet
			ITimeSeriesDescription tsId = association.getTimeSeriesId();

			for (IOutlet outlet : outlets) {
				String str = outlet.toString();
				TimeSeriesIds tsIds = new TimeSeriesIds();
				tsIds.add(tsId.getTimeSeriesId(), association.getOffsetSec());
				tsIds.setLocation(str);
				retval.put(str, tsIds);
			}

		} else {
			String type = ParameterType.getAvailableParameterTypes()[0];
			String interval = Interval.getAvailableIntervals()[0];
			String duration = Duration.getAvailableDurations()[0];
			String version = "MANUAL";
			int offset = UtcOffsetConst.NO_UTC_OFFSET;

			for (IOutlet outlet : outlets) {
				String str = outlet.toString();
				TimeSeriesIds tsIds = new TimeSeriesIds();
				tsIds.setLocation(str);
				tsIds.add("", type, interval, duration, version, offset);

				retval.put(str, tsIds);

			}
		}

		return retval;
	}

}
