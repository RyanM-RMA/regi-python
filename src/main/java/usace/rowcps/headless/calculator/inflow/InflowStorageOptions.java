/*
 * Copyright (c) 2018
 * United States Army Corps of Engineers - Hydrologic Engineering Center (USACE/HEC)
 * All Rights Reserved.  USACE PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval from HEC
 */
package usace.rowcps.headless.calculator.inflow;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import usace.rowcps.data.inflow.InflowDataType;

/**
 * This class is intended to be used for data storage options for Inflow comps and actions. Additional options can be
 * added to this class as needed.
 *
 * @author @author <a href="mailto:ryanm@rmanet.com">Ryan A. Miles (ryanm@rmanet.com)</a>
 */
final class InflowStorageOptions
{

	private final Set<InflowComputationStorageOption> _dataTypesToStore = new HashSet<>();

	public void setComputationStorageOptions(List<InflowComputationStorageOption> options)
	{
		_dataTypesToStore.clear();

		for (InflowComputationStorageOption option : options)
		{
			_dataTypesToStore.add(option);
		}
	}

	public boolean isComputedDataTypeStored(InflowDataType dataType)
	{
		return _dataTypesToStore.stream()
				.anyMatch((option) -> option.getDataType() == dataType);
	}
}
