/*
 * Copyright (c) 2018
 * United States Army Corps of Engineers - Hydrologic Engineering Center (USACE/HEC)
 * All Rights Reserved.  USACE PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval from HEC
 */

package usace.rowcps.headless.calculator.inflow.actions;

import java.util.Calendar;
import java.util.Date;
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

	public HeadlessAutoAdjustInflowsAction(Date date, InflowCache ic, int displayUnits, InflowAdjustedTypeModel adjustM)
	{
		super(date, ic, displayUnits, adjustM);
	}

	@Override
	protected void clearQualityForAdjustedData(InflowDataContainer idcAdjusted)
	{
		if (idcAdjusted != null)
		{
			idcAdjusted.setProtected(false);
			idcAdjusted.setProtectedOverride(false);
		}
	}

	@Override
	protected void setInflowValueForNegativeValue(Calendar gc, Date dateKey, double baseRecess, double recess, InflowDataContainer idcAdjusted)
	{
		super.setInflowValueForNegativeValue(gc, dateKey, baseRecess, recess, idcAdjusted);
		clearQualityForAdjustedData(idcAdjusted);
	}
}
