/*
 * Copyright (c) 2018
 * United States Army Corps of Engineers - Hydrologic Engineering Center (USACE/HEC)
 * All Rights Reserved.  USACE PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval from HEC
 */
package usace.rowcps.headless.calculator.inflow;

import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author Ryan A. Miles (ryanm@rmanet.com)
 */
public class InflowStorageOptionsTest
{
	
	public InflowStorageOptionsTest()
	{
	}

	@Test
	public void testDefaultCtor()
	{
		InflowStorageOptions options = new InflowStorageOptions();
		
		assertTrue(options.isStoringEvapAsFlow());
		assertTrue(options.isStoringProjectReleases());
	}
	
	@Test
	public void testStoreAllComputedData()
	{
		InflowStorageOptions options = InflowStorageOptions.storeAllComputedData();
		assertTrue(options.isStoringEvapAsFlow());
		assertTrue(options.isStoringProjectReleases());
	}
	
	@Test
	public void testDoNotStoreAllComputedData()
	{
		InflowStorageOptions options = InflowStorageOptions.doNotStoreAllComputedData();
		assertFalse(options.isStoringEvapAsFlow());
		assertFalse(options.isStoringProjectReleases());
	}
	
	@Test
	public void testStoreComputedEvapAsFlow()
	{
		InflowStorageOptions options = InflowStorageOptions.doNotStoreAllComputedData();
		
		assertFalse(options.isStoringEvapAsFlow());
		options.storeComputedEvapAsFlow();
		assertTrue(options.isStoringEvapAsFlow());
	}
	
	@Test
	public void testDoNotStoreComputedEvapAsFlow()
	{
		InflowStorageOptions options = InflowStorageOptions.storeAllComputedData();
		
		assertTrue(options.isStoringEvapAsFlow());
		options.doNotStoreComputedEvapAsFlow();
		assertFalse(options.isStoringEvapAsFlow());
	}
	
	@Test
	public void testStoreComputedProjectReleases()
	{
		InflowStorageOptions options = InflowStorageOptions.doNotStoreAllComputedData();
		
		assertFalse(options.isStoringProjectReleases());
		options.storeComputedProjectReleases();
		assertTrue(options.isStoringProjectReleases());
	}
	
	@Test
	public void testDoNotStoreComputedProjectReleases()
	{
		InflowStorageOptions options = InflowStorageOptions.storeAllComputedData();
		
		assertTrue(options.isStoringProjectReleases());
		options.doNotStoreComputedProjectReleases();
		assertFalse(options.isStoringProjectReleases());
	}
	
	@Test
	public void testDoubleStoreThenNotStoreEvapAsFlow()
	{
		InflowStorageOptions options = InflowStorageOptions.storeAllComputedData();
		options.storeComputedEvapAsFlow();
		options.storeComputedEvapAsFlow();
		options.doNotStoreComputedEvapAsFlow();
		assertFalse(options.isStoringEvapAsFlow());
	}
	
	@Test
	public void testDoubleNotStoreThenStoreEvapAsFlow()
	{
		InflowStorageOptions options = InflowStorageOptions.storeAllComputedData();
		options.doNotStoreComputedEvapAsFlow();
		options.doNotStoreComputedEvapAsFlow();
		options.storeComputedEvapAsFlow();
		assertTrue(options.isStoringEvapAsFlow());
	}
	
	@Test
	public void testDoubleStoreThenNotStoreReleases()
	{
		InflowStorageOptions options = InflowStorageOptions.storeAllComputedData();
		
		options.storeComputedProjectReleases();
		options.storeComputedProjectReleases();
		options.doNotStoreComputedProjectReleases();
		assertFalse(options.isStoringProjectReleases());
	}
	
	@Test
	public void testDoubleNotStoreThenStoreReleases()
	{
		InflowStorageOptions options = InflowStorageOptions.storeAllComputedData();
		
		options.doNotStoreComputedProjectReleases();
		options.doNotStoreComputedProjectReleases();
		options.storeComputedProjectReleases();
		assertTrue(options.isStoringProjectReleases());
	}
}
