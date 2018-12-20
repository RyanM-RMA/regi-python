/*
 * Copyright (c) 2018
 * United States Army Corps of Engineers - Hydrologic Engineering Center (USACE/HEC)
 * All Rights Reserved.  USACE PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval from HEC
 */
package usace.rowcps.headless;

import java.util.TimeZone;
import org.junit.Test;

/**
 * This class is intended for use by developers to run headless after making changes. Run a single test at a time
 * otherwise it might be a while...
 * 
 * All files are relative to the JYTHON_FILE_ROOT variable, which should be in this same folder.
 *
 * @author @author <a href="mailto:ryanm@rmanet.com">Ryan A. Miles (ryanm@rmanet.com)</a>
 */
public class TestHeadless
{
	private static final String JYTHON_FILE_ROOT = "src\\test\\java\\usace\\rowcps\\headless\\";
	private static final String CREDENTIALS_FILE = "credentials.properties";

	@Test
	public void testGateSettingsPy() throws Exception
	{
		RegiCLI.runHeadlessTest(getArgsForFile("GateSettings2.py"));
	}

	@Test
	public void testStatusDemoPy() throws Exception
	{
		RegiCLI.runHeadlessTest(getArgsForFile("StatusDemo.py"));
	}

	@Test
	public void testGateFlowCalc2Py() throws Exception
	{
		RegiCLI.runHeadlessTest(getArgsForFile("GateFlowCalc2.py"));
	}

	@Test
	public void testPoolPercentCalcPy() throws Exception
	{
		RegiCLI.runHeadlessTest(getArgsForFile("PoolPercentCalc.py"));
	}

	@Test
	public void testInflowCalcClonePy() throws Exception
	{
		RegiCLI.runHeadlessTest(getArgsForFile("InflowCalcClone.py"));
	}

	@Test
	public void testInflowCalcZeroNegativePy() throws Exception
	{
		RegiCLI.runHeadlessTest(getArgsForFile("InflowCalcZeroNegative.py"));
	}

	@Test
	public void testInflowCalcComputedInflowPy() throws Exception
	{
		RegiCLI.runHeadlessTest(getArgsForFile("InflowCalcComputedInflow.py"));
	}

	@Test
	public void testInflowCalcComputeEvapAsFlowPy() throws Exception
	{
		RegiCLI.runHeadlessTest(getArgsForFile("InflowCalcComputeEvapAsFlow.py"));
	}

	private String[] getArgsForFile(String file)
	{
		return getArgsForFileAndTimeZone(file, TimeZone.getTimeZone("America/Chicago"));
	}

	private String[] getArgsForFileAndTimeZone(String file, TimeZone tz)
	{
		String[] args = new String[]
		{
			"-Drowcps.timezone=" + tz.getID(),
			"-p", JYTHON_FILE_ROOT + CREDENTIALS_FILE,
			"-f", JYTHON_FILE_ROOT + file,
		};

		return args;
	}
}
