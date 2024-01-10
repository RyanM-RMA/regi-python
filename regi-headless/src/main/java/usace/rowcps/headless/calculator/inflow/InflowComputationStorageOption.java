/*
 * Copyright (c) 2018
 * United States Army Corps of Engineers - Hydrologic Engineering Center (USACE/HEC)
 * All Rights Reserved.  USACE PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval from HEC
 */

package usace.rowcps.headless.calculator.inflow;

import usace.rowcps.data.inflow.InflowDataType;

/**
 * Represents a single storage option for computed values.
 * 
 * @author @author <a href="mailto:ryanm@rmanet.com">Ryan A. Miles (ryanm@rmanet.com)</a>
 */
public enum InflowComputationStorageOption
{
	EVAP_AS_FLOW(InflowDataType.ProjectEvapAsFlow),
	PROJECT_RELEASES(InflowDataType.AverageRelease);
	
	private final InflowDataType _dataType;

	InflowComputationStorageOption(InflowDataType _dataType)
	{
		this._dataType = _dataType;
	}

	InflowDataType getDataType()
	{
		return _dataType;
	}
}
