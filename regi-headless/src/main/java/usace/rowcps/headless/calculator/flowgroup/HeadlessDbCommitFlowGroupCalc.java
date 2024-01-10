/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package usace.rowcps.headless.calculator.flowgroup;

import hec.data.TimeWindow;
import hec.data.UsgsRounder;
import hec.data.location.LocationTemplate;
import hec.data.project.AtProjectDescriptor;
import hec.data.tx.DataSetTx;
import hec.db.DbConnectionException;
import hec.db.DbIoException;

import java.io.IOException;
import java.io.StringReader;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import usace.rowcps.computation.flowgroup.DbCommitFlowGroupCalc;
import usace.rowcps.computation.flowgroup.FlowGroupTimeSeriesComputationInfo;
import usace.rowcps.headless.LoggingOptions;
import usace.rowcps.regi.model.CacheUsage;
import usace.rowcps.regi.model.ManagerId;
import usace.rowcps.regi.model.OptionalParams;

/**
 *
 * @author Ryan A. Miles (ryanm@rmanet.com)
 */
public class HeadlessDbCommitFlowGroupCalc extends DbCommitFlowGroupCalc
{
	private static final Logger LOGGER = Logger.getLogger(HeadlessDbCommitFlowGroupCalc.class.getName());
	
	public HeadlessDbCommitFlowGroupCalc(AtProjectDescriptor projectDescriptor)
	{
		super(projectDescriptor);
	}

	@Override
	public void calcTimeSeries(ManagerId managerId, LocationTemplate sharedLocRef, LocationTemplate assignedLocRef, String flowGroupId, Date startDate, Date endDate, CacheUsage cacheUsage, OptionalParams options, TimeZone tz) throws DbIoException, DbConnectionException
	{
		super.calcTimeSeries(managerId, sharedLocRef, assignedLocRef, flowGroupId, startDate, endDate, cacheUsage, options, tz); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	protected void logFlowGroupCalculations(Map<DataSetTx, FlowGroupTimeSeriesComputationInfo> computationData, TimeWindow timeWindow, TimeZone timeZone)
	{
		if (!LoggingOptions.isFlowGroupCompLoggingEnabled())
		{
			return;
		}

		for (Map.Entry<DataSetTx, FlowGroupTimeSeriesComputationInfo> entry : computationData.entrySet())
		{
			FlowGroupTimeSeriesComputationInfo computationInfo = entry.getValue();
			try
			{
				TreeSet<Date> dates = new TreeSet<>(computationInfo.getFlowGroupTimeSeries()
																   .getFlowGroupTimeSeriesStartTimes(timeWindow, timeZone));
				UsgsRounder rounder = UsgsRounder.getDefault();
				Html2Text parser = new Html2Text();
				StringBuilder nonHtmlSb = new StringBuilder();

				for (Date date : dates)
				{
					String html = "";
					if (LoggingOptions.isFullFlowGroupCompLoggingEnabled())
					{
						html = computationInfo.getFullComputationInfo(date, timeZone, rounder);
					}
					else if (LoggingOptions.isAbridgedFlowGroupCompLoggingEnabled())
					{
						html = computationInfo.getAbridgedComputationInfo(date, timeZone, rounder);
					}

					parser.parse(new StringReader(html));
					String nonHtml = parser.getText();
					nonHtmlSb.append(nonHtml.trim())
							 .append(System.lineSeparator());
				}
				LOGGER.log(Level.SEVERE, nonHtmlSb.toString());
			}
			catch (mil.army.usace.hec.metadata.DataSetIllegalArgumentException | IOException ex)
			{
				LOGGER.log(Level.SEVERE, "Unable to log computation info.", ex);
			}
		}
	}
}
