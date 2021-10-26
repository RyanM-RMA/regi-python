/*
 * Copyright (c) 2018
 * United States Army Corps of Engineers - Hydrologic Engineering Center (USACE/HEC)
 * All Rights Reserved.  USACE PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval from HEC
 */

package usace.rowcps.headless.calculator.inflow;

import java.util.TimeZone;
import usace.rowcps.computation.inflow.InflowDataAdapter;
import usace.rowcps.regi.model.ManagerId;
import hec.data.project.AtProjectDescriptor;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import usace.rowcps.computation.inflow.TsDataColumn;
import usace.rowcps.data.flowgroup.FlowGroupTimeSeries;
import usace.rowcps.data.tabs.dailyops.GlobalDailyOpsOptionsBuilder;
import usace.rowcps.regi.model.OptionalParams;

/**
 *
 * @author @author <a href="mailto:ryanm@rmanet.com">Ryan A. Miles (ryanm@rmanet.com)</a>
 */
public class HeadlessInflowDataAdapter extends InflowDataAdapter
{

	
	public HeadlessInflowDataAdapter(AtProjectDescriptor projectDescriptor, ManagerId managerId, TimeZone projectTimeZone)
	{
		super(projectDescriptor, managerId, projectTimeZone, GlobalDailyOpsOptionsBuilder.buildDefaultOptions(projectDescriptor.getOfficeId()));
	}

	@Override
	protected TsDataColumn retrieveAvgReleaseData(FlowGroupTimeSeries fgts, Date startTime, Date endTime, Date lastElevDate, TimeZone projectTimeZone, OptionalParams options)
			throws hec.db.DbConnectionException, hec.db.DbIoException, hec.data.DataSetIllegalArgumentException
	{
		Logger.getLogger(HeadlessInflowDataAdapter.class.getName()).log(Level.FINE, "This is intended to be ignored under certain circumstances");
		return new TsDataColumn();
	}
}
