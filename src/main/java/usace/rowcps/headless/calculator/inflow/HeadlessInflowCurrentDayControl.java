/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package usace.rowcps.headless.calculator.inflow;

import com.rma.model.Project;
import hec.data.DataSetException;
import hec.data.ITimeSeriesDescription;
import hec.data.location.LocationTemplate;
import hec.data.project.IProject;
import hec.data.tx.DataSetTx;
import hec.data.tx.DataSetTxIllegalArgumentException;
import hec.data.tx.DataSetTxTemplate;
import hec.data.tx.DescriptionTx;
import hec.db.DbConnectionException;
import hec.db.DbException;
import hec.db.DbIoException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import rma.util.RMAConst;
import usace.rowcps.computation.inflow.InflowComputation;
import usace.rowcps.data.association.IAssociationProvider;
import usace.rowcps.data.association.ITimeSeriesAssociation;
import usace.rowcps.data.project.TsUsageId;
import usace.rowcps.regi.interfaces.model.ICurrentDayControl;
import usace.rowcps.regi.model.AtAssociationCache;
import usace.rowcps.regi.model.AtTimeSeriesManager;
import usace.rowcps.regi.model.CacheUsage;
import usace.rowcps.regi.model.ManagerId;
import usace.rowcps.regi.model.OptionalParams;
import usace.rowcps.regi.model.RegiDomain;
import usace.rowcps.regi.status.AtProjectManager;

/**
 *
 * @author Ryan A. Miles (ryanm@rmanet.com)
 */
public class HeadlessInflowCurrentDayControl implements ICurrentDayControl
{
	private final Date _startDate;
	private final int _lookForward;

	public HeadlessInflowCurrentDayControl(ManagerId manId, Date currentDate, TimeZone projectTimeZone, LocationTemplate locRef)
			throws DbException, DbConnectionException, DbIoException, DataSetTxIllegalArgumentException, DataSetException
	{
		Calendar startCal = Calendar.getInstance(projectTimeZone);
		Calendar endCal = Calendar.getInstance(projectTimeZone);
		
		NavigableSet<Date> dates = getDatesInMonth(manId, currentDate, projectTimeZone, locRef);

		startCal.setTime(dates.first());
		endCal.setTime(dates.last());

		_lookForward = endCal.get(Calendar.DATE) - startCal.get(Calendar.DATE);
		_startDate = dates.first();
	}

	private NavigableSet<Date> getDatesInMonth(ManagerId managerId, Date currentDate, TimeZone projectTimeZone, LocationTemplate locRef)
			throws DbException, DbConnectionException, DbIoException, DataSetTxIllegalArgumentException, DataSetException
	{
		List<Date> dates = InflowComputation.getDatesInMonth(currentDate, projectTimeZone);

		NavigableSet<Date> navDates = new TreeSet<>(dates);
		//This is a weird headless bug in the getDatesInMonth.  It adds an extra date we don't need.
		navDates.remove(navDates.last());

		Date lastElevDataDate = findLastElevDataDate(managerId, navDates, locRef);
		
		if (lastElevDataDate != null)
		{
			//This is sub-daily, so go with a date above it or equal to it.
			Date key = navDates.ceiling(lastElevDataDate);
			
			if (key == null)
			{
				key = lastElevDataDate;
			}
			
			NavigableSet<Date> tempDates = navDates.subSet(navDates.first(), true, lastElevDataDate, true);
			
			if (!tempDates.isEmpty())
			{
				navDates = tempDates;
			}
		}
		
		return navDates;
	}

	@Override
	public void setDate(Date date, boolean fireEvents)
	{
		//No op
	}

	@Override
	public Date getCurrentDate()
	{
		return _startDate;
	}

	@Override
	public int getLookbackDays()
	{
		return 0;
	}

	@Override
	public int getLookForwardDays()
	{
		return _lookForward;
	}

	private Date findLastElevDataDate(ManagerId manId, NavigableSet<Date> dates, LocationTemplate locRef)
			throws DbException, DbConnectionException, DbIoException, DataSetTxIllegalArgumentException, DataSetException
	{
		IProject project = retrieveProject(manId, locRef);
		DataSetTx dstx = getElevTimeSeries(manId, dates.first(), dates.last(), project);
		
		return findLastDataDate(dstx);
	}
	
	private DataSetTx getElevTimeSeries(ManagerId manId, Date startDate, Date endDate, IProject project)
			throws DbException, DbConnectionException, DbIoException, DataSetTxIllegalArgumentException, DataSetException
	{
		DataSetTx output = null;
		
		Project prj = RegiDomain.getCurrentProject();
		
		if (prj instanceof RegiDomain)
		{
			DataSetTxTemplate tsId = makeTemplateForAssociation(manId, TsUsageId.INFLOWPANEL_PROJECT_ELEVATION, startDate, endDate, project);
			
			if(tsId != null)
			{
				AtTimeSeriesManager tsMan = ((RegiDomain) prj).getAtTimeSeriesManager(manId);
				output = tsMan.retrieveDataSetTx(tsId, CacheUsage.NORMAL, OptionalParams.createForMetrics("Headless current day control retrieval."));
			}
			else
			{
				Logger.getLogger(HeadlessInflowCurrentDayControl.class.getName()).log(Level.SEVERE, "Required elevation time series association is missing for {0}", project.getProjectId());
			}
		}
		
		return output;
	}
	
	private DescriptionTx getDescriptionFromAssociation(ManagerId managerId, TsUsageId usageId, IProject project)
			throws DataSetTxIllegalArgumentException
	{
		
		RegiDomain prj = (RegiDomain)RegiDomain.getCurrentProject();
		AtAssociationCache atAssociationCache = prj.getAtAssociationCache(managerId);
		IAssociationProvider<ITimeSeriesAssociation> tsAssocProvider = atAssociationCache.getTimeSeriesAssociationsProvider(project.getLocation().getLocationTemplate());
		ITimeSeriesAssociation tsAssoc = null;
		ITimeSeriesDescription tsId = null;
		DescriptionTx output = null;

		if(tsAssocProvider != null)
		{
			tsAssoc = tsAssocProvider.getInputAssociation(usageId.getUsageId());
		}

		if (tsAssoc != null)
		{
			tsId = tsAssoc.getTimeSeriesId();
		}
		
		if (tsId != null)
		{
			output = tsId.getLookup().lookup(DescriptionTx.class);
		}
		
		return output;
	}
	
	private DataSetTxTemplate makeTemplateForAssociation(ManagerId managerId, TsUsageId usageId, Date startDate, Date endDate, IProject project)
			throws DataSetTxIllegalArgumentException
	{
		DescriptionTx desc = getDescriptionFromAssociation(managerId, usageId, project);
		DataSetTxTemplate template = null;
		
		if (desc != null)
		{
			template = new DataSetTxTemplate(desc, startDate.getTime(), endDate.getTime());
		}
		
		return template;
	}
	
	private IProject retrieveProject(ManagerId manId, LocationTemplate locRef)
			throws DbConnectionException, DbIoException
	{
		IProject output = null;
		
		Project prj = RegiDomain.getCurrentProject();
		
		if (prj instanceof RegiDomain)
		{
			AtProjectManager prjMan = ((RegiDomain) prj).getAtProjectManager(manId);
			output = prjMan.getIProject(locRef, CacheUsage.NORMAL);
		}
		
		return output;
	}

	private Date findLastDataDate(DataSetTx dstx)
	{
		Date output = null;
		
		if (dstx != null)
		{
			NavigableMap<Date, Double> dataMap = dstx.getDateValueNavigableMap();
			
			for (Date date : dataMap.descendingKeySet())
			{
				Double value = dataMap.get(date);
				
				if (value != null && RMAConst.isValidValue(value))
				{
					//First date in the descending data with real data is the first real data date.
					output = date;
					break;
				}
			}
		}
		
		return output;
	}
}
