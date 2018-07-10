/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package usace.rowcps.headless.calculator.flowgroup;

import hec.data.location.LocationTemplate;
import hec.db.DbConnectionException;
import hec.db.DbIoException;
import java.util.Date;
import java.util.TimeZone;
import usace.rowcps.computation.flowgroup.DbCommitFlowGroupCalc;
import usace.rowcps.data.project.AtProjectDescriptor;
import usace.rowcps.regi.model.CacheUsage;
import usace.rowcps.regi.model.ManagerId;
import usace.rowcps.regi.model.OptionalParams;

/**
 *
 * @author Ryan A. Miles (ryanm@rmanet.com)
 */
public class HeadlessDbCommitFlowGroupCalc extends DbCommitFlowGroupCalc
{
	
	public HeadlessDbCommitFlowGroupCalc(AtProjectDescriptor projectDescriptor)
	{
		super(projectDescriptor);
	}

	@Override
	public void calcTimeSeries(ManagerId managerId, LocationTemplate sharedLocRef, LocationTemplate assignedLocRef, String flowGroupId, Date startDate, Date endDate, CacheUsage cacheUsage, OptionalParams options, TimeZone tz) throws DbIoException, DbConnectionException
	{
		super.calcTimeSeries(managerId, sharedLocRef, assignedLocRef, flowGroupId, startDate, endDate, cacheUsage, options, tz); //To change body of generated methods, choose Tools | Templates.
	}
	
}
