/*
 * Copyright (c) 2018
 * United States Army Corps of Engineers - Hydrologic Engineering Center (USACE/HEC)
 * All Rights Reserved.  USACE PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval from HEC
 */
package usace.rowcps.headless.calculator.inflow.actions;

import java.util.Date;
import java.util.List;
import usace.rowcps.computation.inflow.InflowAdjustedTypeModel;
import usace.rowcps.computation.inflow.InflowCache;
import usace.rowcps.computation.inflow.ZeroNegativeAdjustedInflowsAction;
import usace.rowcps.data.inflow.InflowDataContainer;

/**
 *
 * @author @author <a href="mailto:ryanm@rmanet.com">Ryan A. Miles (ryanm@rmanet.com)</a>
 */
public class HeadlessZeroNegativeAdjustedInflowsAction extends ZeroNegativeAdjustedInflowsAction
{

	public HeadlessZeroNegativeAdjustedInflowsAction(Date date, InflowCache ic, int displayU, InflowAdjustedTypeModel adjustModel)
	{
		super(date, ic, displayU, adjustModel);
	}

	@Override
	protected void updateQualityForDataContainers(List<InflowDataContainer> dataContainers)
	{
		for (InflowDataContainer idc : dataContainers)
		{
			idc.clearCachedQuality();
			idc.setQualityToLinearInterpolation();
			idc.setProtected(false);
			idc.setProtectedOverride(false);
		}
	}
}
