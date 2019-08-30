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
import usace.rowcps.computation.inflow.TsDataColumn;
import usace.rowcps.data.flowgroup.FlowGroupTimeSeries;
import usace.rowcps.regi.model.OptionalParams;
import usace.rowcps.regi.model.RegiDomain;

/**
 *
 * @author @author <a href="mailto:ryanm@rmanet.com">Ryan A. Miles (ryanm@rmanet.com)</a>
 */
public class HeadlessInflowDataAdapter extends InflowDataAdapter
{

	private final boolean _retrieveAverageReleases;
	
	public HeadlessInflowDataAdapter(AtProjectDescriptor projectDescriptor, ManagerId managerId, TimeZone projectTimeZone, boolean retrieveAverageReleases)
	{
		super(projectDescriptor, managerId, projectTimeZone);
		_retrieveAverageReleases = retrieveAverageReleases;
	}

	@Override
	protected TsDataColumn retrieveAvgReleaseData(FlowGroupTimeSeries fgts, RegiDomain currentProject, Date startTime, Date endTime, TimeZone projectTimeZone, OptionalParams options) throws hec.db.DbConnectionException, hec.db.DbIoException, hec.data.DataSetIllegalArgumentException
	{
		if (_retrieveAverageReleases)
		{
			return super.retrieveAvgReleaseData(fgts, currentProject, startTime, endTime, projectTimeZone, options);
		}

		return new TsDataColumn();
	}
}
