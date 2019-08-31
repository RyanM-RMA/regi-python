/*
 * Copyright (c) 2018
 * United States Army Corps of Engineers - Hydrologic Engineering Center (USACE/HEC)
 * All Rights Reserved.  USACE PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval from HEC
 */

package usace.rowcps.headless.calculator.inflow.actions;

import java.util.Calendar;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import usace.rowcps.computation.inflow.AutoAdjustInflowsAction;
import usace.rowcps.computation.inflow.InflowAdjustedTypeModel;
import usace.rowcps.computation.inflow.InflowCache;
import usace.rowcps.data.inflow.InflowDataContainer;

/**
 *
 * @author @author <a href="mailto:ryanm@rmanet.com">Ryan A. Miles (ryanm@rmanet.com)</a>
 */
public class HeadlessAutoAdjustInflowsAction extends AutoAdjustInflowsAction
{

	private static final Logger LOGGER = Logger.getLogger(HeadlessAutoAdjustInflowsAction.class.getName());
	public HeadlessAutoAdjustInflowsAction(Date date, InflowCache ic, int displayUnits, InflowAdjustedTypeModel adjustM)
	{
		super(date, ic, displayUnits, adjustM);
	}

	@Override
	protected void clearQualityForAdjustedData(InflowDataContainer idcAdjusted)
	{
		LOGGER.log(Level.FINE, "Purposefully left blank since this affects data quality.");
	}

	@Override
	protected void setAdjustedInflowValue(InflowDataContainer idcAdjusted, double valAdjusted)
	{
		if (idcAdjusted != null && !idcAdjusted.isProtected())
		{
			super.setAdjustedInflowValue(idcAdjusted, valAdjusted);
		}
	}

	@Override
	protected void setInflowValueForNegativeValue(Calendar gc, Date dateKey, double baseRecess, double recess, InflowDataContainer idcAdjusted)
	{
		if (!idcAdjusted.isProtected())
		{
			super.setInflowValueForNegativeValue(gc, dateKey, baseRecess, recess, idcAdjusted);
		}
	}
}
