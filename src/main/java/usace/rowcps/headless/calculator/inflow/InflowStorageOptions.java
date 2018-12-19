/*
 * Copyright (c) 2018
 * United States Army Corps of Engineers - Hydrologic Engineering Center (USACE/HEC)
 * All Rights Reserved.  USACE PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval from HEC
 */
package usace.rowcps.headless.calculator.inflow;

import java.util.HashSet;
import java.util.Set;
import usace.rowcps.data.inflow.InflowDataType;

/**
 * This class is intended to be used for data storage options for Inflow comps and actions.
 * 
 * @author @author <a href="mailto:ryanm@rmanet.com">Ryan A. Miles (ryanm@rmanet.com)</a>
 */
public final class InflowStorageOptions
{

	private final Set<InflowDataType> _dataTypesToStore = new HashSet<>();
	
	public InflowStorageOptions()
	{
		this(true);
	}
	
	private InflowStorageOptions(boolean storeAdditionalData)
	{
		if (storeAdditionalData)
		{
			storeComputedEvapAsFlow();
			storeComputedProjectReleases();
		}
	}
	
	public static InflowStorageOptions storeAllComputedData()
	{
		return new InflowStorageOptions(true);
	}
	
	public static InflowStorageOptions doNotStoreAllComputedData()
	{
		return new InflowStorageOptions(false);
	}
	
	boolean isStoringEvapAsFlow()
	{
		return _dataTypesToStore.contains(InflowDataType.ProjectEvapAsFlow);
	}
	
	boolean isStoringProjectReleases()
	{
		return _dataTypesToStore.contains(InflowDataType.AverageRelease);
	}
	
	public void storeComputedEvapAsFlow()
	{
		_dataTypesToStore.add(InflowDataType.ProjectEvapAsFlow);
	}
	
	public void storeComputedProjectReleases()
	{
		_dataTypesToStore.add(InflowDataType.AverageRelease);
	}
	
	public void doNotStoreComputedEvapAsFlow()
	{
		_dataTypesToStore.remove(InflowDataType.ProjectEvapAsFlow);
	}
	
	public void doNotStoreComputedProjectReleases()
	{
		_dataTypesToStore.remove(InflowDataType.AverageRelease);
	}
}
