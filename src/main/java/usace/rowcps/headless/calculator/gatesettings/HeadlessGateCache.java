/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package usace.rowcps.headless.calculator.gatesettings;

import hec.data.location.Location;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import usace.rowcps.computation.common.IEventThreadExceptionProcessor;
import usace.rowcps.computation.common.IThreadedBlockRetriever;
import usace.rowcps.computation.common.grouping.IControlledOutlet;
import usace.rowcps.computation.common.grouping.IControlledOutletGroup;
import usace.rowcps.computation.gatesettings.CompoundOutletHolder;
import usace.rowcps.computation.gatesettings.GateSettingsDataAdapter;
import usace.rowcps.computation.gatesettings.common.AggregateGateOpeningEntry;
import usace.rowcps.computation.gatesettings.common.GateCache;
import usace.rowcps.computation.gatesettings.common.GateMergeException;
import usace.rowcps.computation.gatesettings.common.GateOpeningEntry;
import usace.rowcps.computation.gatesettings.common.GateSettingsBlock;
import usace.rowcps.data.outlet.IOutlet;
import usace.rowcps.data.project.AtProjectDescriptor;
import usace.rowcps.regi.interfaces.model.ICurrentDayControl;
import usace.rowcps.regi.model.ManagerId;

/**
 * This class is intended to hold onto all of the headless specific operations for the gate cache.
 * @author Ryan A. Miles (ryanm@rmanet.com)
 */
public class HeadlessGateCache extends GateCache
{
	private final Date _startDate;
	private final Date _endDate;

	public HeadlessGateCache(Date _startDate, Date _endDate, ManagerId managerId, AtProjectDescriptor projectDescriptor, int recordsToCacheInThread, ICurrentDayControl currentDayControl, IEventThreadExceptionProcessor eventThreadExceptionProcessor, IThreadedBlockRetriever completionCallbackTarget, Set<Date> modifiedGateSettingDates)
	{
		super(managerId, projectDescriptor, recordsToCacheInThread, currentDayControl, eventThreadExceptionProcessor, completionCallbackTarget, modifiedGateSettingDates);
		this._startDate = _startDate;
		this._endDate = _endDate;
	}

	@Override
	protected void updateHeadTimeWindow(Calendar start, Calendar end, SortedMap<Date, ? extends Object> currentCache)
	{
		start.setTime(_startDate);
		end.setTime(_endDate);
	}

	boolean modifyGateOpeningBlock(Date dateKey, IControlledOutlet outlet, Double opening) throws GateMergeException
	{
		boolean retval = false;

		GateSettingsBlock gateSettingBlock = this.getGateSetting(dateKey);
		if (gateSettingBlock == null)
		{
			ArrayList<AggregateGateOpeningEntry> aggregateOpenings = new ArrayList<>();
			gateSettingBlock = new GateSettingsBlock(getOutletGroupContainer().getOutletGroups(), aggregateOpenings);
			putGateSetting(dateKey, gateSettingBlock);
		}

		IControlledOutletGroup outletGroup = outlet.getParent();
		List<AggregateGateOpeningEntry> aggregateOpenings = gateSettingBlock.getReadOnlyAggregateOpenings();
		GateOpeningEntry gateOpeningEntry = null;
		for (AggregateGateOpeningEntry aggregateOpening : aggregateOpenings)
		{
			if (aggregateOpening.getOutletGroup() == outletGroup)
			{
				// same gate type...
				List<GateOpeningEntry> gateSettings = aggregateOpening.getReadOnlyGateSettings();
				for (int i = 0; i < gateSettings.size(); i++)
				{
					if (gateSettings.get(i).getOutlet() == outlet)
					{
						// found the gate setting that need to be removed from this group and added to another one
						gateOpeningEntry = gateSettings.get(i);
						if (gateSettings.size() == 1)
						{
							// since this is the only gate entry in this group we can just change the opening for the group...
							if (isClosedGateOpening(opening))
							{
								gateSettingBlock.removeAggregateOpening(aggregateOpening);
							}
							else
							{
								aggregateOpening.setGateOpeningCommon(opening);
							}
						}
						else
						{
							// remove the gate from the current outlet location
							aggregateOpening.removeGate(outlet);

							// see if we can insert the gate into a group of the same type wih the same opening.
							boolean found = false;
							for (AggregateGateOpeningEntry aggregateOpening2 : aggregateOpenings)
							{
								if (aggregateOpening2.getOutletGroup() == outletGroup
										&& aggregateOpening2.getGateOpeningCommon() == opening)
								{
									// good. we found it. no need to create a new group...
									aggregateOpening2.addGate(outlet);
									found = true;
									break;
								}
							}
							if (!found)
							{
								//                                // we need to add a whole noew AggregateGateOpening entry
								//                                ArrayList<GateOpeningEntry> newGateOpeningEntries = new ArrayList<GateOpeningEntry>();
								//                                newGateOpeningEntries.add(gateOpeningEntry);
								//                                AggregateGateOpeningEntry newAggregateGateOpeningEntry = new AggregateGateOpeningEntry(outletGroup, opening, newGateOpeningEntries);
								//                                gateOpeningEntry.setParent(newAggregateGateOpeningEntry);
								//                                gateSettingBlock.addAggregateOpeningsElement(newAggregateGateOpeningEntry);
								gateOpeningEntry = null;
								break;
							}
						}
						break;
					}
				}
			}
			if (gateOpeningEntry != null)
			{
				retval = true;
				break;
			}
		}
		// ok. if we get to here and the gateOpeningEntry is still null then we didn't find the outlet being
		// used yet. this means it was probably 0 originally and now it is not. The easiest way is just to add a whole new aggregate entry block even if it is the same
		// opening as other ones...the gatecache consolidateGateCache() call will straighten this out later...
		if (gateOpeningEntry == null && !isClosedGateOpening(opening))
		{
			// we need to add a whole noew AggregateGateOpening entry
			AggregateGateOpeningEntry newAggregateGateOpeningEntry = new AggregateGateOpeningEntry(outletGroup, opening,
					new ArrayList<>());
			gateOpeningEntry = new GateOpeningEntry(newAggregateGateOpeningEntry, outlet);
			gateOpeningEntry.setParent(newAggregateGateOpeningEntry);
			gateOpeningEntry.addWeakPropertyChangeListener(newAggregateGateOpeningEntry);
			newAggregateGateOpeningEntry.addGateSetting(gateOpeningEntry);

			gateSettingBlock.addAggregateOpeningsElement(newAggregateGateOpeningEntry);

			//if this is a compound outlet, need to add other aggregate openings for each component
			addOtherCompoundOutletMembers(outletGroup, dateKey, gateSettingBlock);

			newAggregateGateOpeningEntry.addWeakPropertyChangeListener(gateSettingBlock);
			retval = true;
		}

		if (retval)
		{
			gateSettingBlock.setModified(true);
			this.checkGateRules(dateKey);
		}

		return retval;
	}

	private void addOtherCompoundOutletMembers(IControlledOutletGroup outletGroup, Date dateKey, GateSettingsBlock gsb)
	{

		CompoundOutletHolder holder = this.getCompoundOutletHolder();

		//make sure this is a compound outlet before proceeding
		if (!holder.containsOutletInRatingGroup(outletGroup.getGroupName()))
		{
			return;
		}

		for (IOutlet outlet : holder.getOrderedOutlets(holder.getNameByRatingGroup(outletGroup.getGroupName())))
		{
			boolean exists = false;
			final Location outLoc = outlet.getLocation();

			//check to make sure this outlet exists
			for (int k = 0; k < gsb.getAggregateOpeningsSize(); k++)
			{

				String outletName = gsb.getAggregateOpeningsElement(k).getOutletGroup().getOutlets().get(0).getOutletName();

				if (outLoc.getSubLocationId().equals(outletName))
				{
					exists = true;
					break;
				}
			}

			if (!exists)
			{
				final AggregateGateOpeningEntry agoe
						= createGateSettingPersistingFromPrevious(outlet, outlet.getRatingGroupRef().getName(), dateKey);
				gsb.addAggregateOpeningsElement(agoe);
			}
		}
	}

	private AggregateGateOpeningEntry createGateSettingPersistingFromPrevious(IOutlet outlet, String ratingGroupName, Date dateKey)
	{
		AggregateGateOpeningEntry retval;

		final String outletName = outlet.getLocation().getLongName();
		// This method based on a similar method in FineTuningModel.   the FineTuningModel version does this:
		// findColumn containing outlet.getLocation().getLongName() and where column is also a GateOpening.
		// The columns are built by:
		// iterating outletGroups
		//	iterating the IControlledOutlets in each group
		//    inserting a column for GateOpening and a column for Release.
		//
		// Integer colIndex = getColumnIndexForOpeningFromOutletName(outletName);    // Whats the GateCache equiv version of this?
		IControlledOutlet found = getOutlet(outletName);

		// FineTuning did this:
		//int rowIndex = getRowIndexFromDate(dateKey);
		// and the findTunging woudl use rowIndex-1
		// thats basically the same as:
		Date prevDate = this.getPrevGateSettingKey(dateKey);

		// FineTunign did:
//		  ////// If this method cannot find the previous setting, then wide open is a reasonable default
//        if (colIndex == null || rowIndex < 1) {
//            return this.createWideOpenGateSetting(outletName);
//        }
// if we can't find a
		if (found == null || prevDate == null)
		{
			retval = createWideOpenGateSetting(outletName);
		}
		else
		{

			Object previousOpeningObject = this.getGateSetting(prevDate).getGateOpening(outletName, ratingGroupName);//getValueAt(rowIndex-1, colIndex);

			if (previousOpeningObject == null || !(previousOpeningObject instanceof Double))
			{
				//got the wrong cell somehow, or there is no persistent setting ... try just getting the previous value in the table.

				// FineTuning did this:
//            Object opening = getValueAt(rowIndex-1, colIndex);
// here is what getValueAt would do:
/*
         Outlet outlet = (Outlet) colDef.getRefObject();
                        Double opening = _gateCache.getGateOpening(ftre.getDateKey(), outlet);
                        if(opening == null || Double.isNaN(opening))
                        {
                            opening =
                            outlet.getParent().getMinRatingExtent(IndependantParamRatingObjectType.GateOpening);
                        }
                        opening = UnitsHelper.convertToDisplayUnits(opening, _displayUnits, outlet.getSiUnits(), outlet.
                                getEnglishUnits());
                        objRet = opening;
				 */
				// but we already know that colIndex points at a GateOpening column
				// the refObject is always going to be the IControlledOutlet that the column was built from.
				// we already have that object here in variable "found"
				// ftre is just the row element at rowIndex-1.   gettting the Datekey from that is just prevDate
				Double opening = this.getGateOpening(prevDate, found);
				if (opening == null || Double.isNaN(opening))
				{
					opening = found.getParent().getMinRatingExtent(GateSettingsDataAdapter.IndependantParamRatingObjectType.GateOpening);
				}

//				FineTuneing did that but i don't think we have to.
//				opening = UnitsHelper.convertToDisplayUnits(opening, _displayUnits, found.getSiUnits(), found.
//					getEnglishUnits());
				// and getValueAt would get the row for rowIndex and then call gateCache.getOpening
				if (!(opening instanceof Double))
				{

					//tried all options. just create a wide open setting
					return this.createWideOpenGateSetting(outletName);
				}

				previousOpeningObject = opening;
			}

			double previousOpening = (double) previousOpeningObject;

			IControlledOutletGroup group = getOutletGroupContainer().getOutletGroup(ratingGroupName);

			ArrayList<GateOpeningEntry> openingEntry = new ArrayList<>();
			openingEntry.add(new GateOpeningEntry(group.getOutlets().get(0)));

			retval = new AggregateGateOpeningEntry(group, previousOpening, openingEntry);
		}
		return retval;
	}
}
