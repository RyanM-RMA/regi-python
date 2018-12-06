/*
 * Copyright (c) 2018
 * United States Army Corps of Engineers - Hydrologic Engineering Center (USACE/HEC)
 * All Rights Reserved.  USACE PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval from HEC
 */
package usace.rowcps.headless;

/**
 *
 * @author @author <a href="mailto:ryanm@rmanet.com">Ryan A. Miles (ryanm@rmanet.com)</a>
 */
public class DebugMain
{

	public static void main(String[] args)
	{
		args = new String[]
		{
			"-Drowcps.timezone=America/Chicago",
			"-p", "src\\test\\java\\usace\\rowcps\\headless\\credentials.properties",
			"-f", "src\\test\\java\\usace\\rowcps\\headless\\GateSettings2.py",
//			"-f", "src\\test\\java\\usace\\rowcps\\headless\\StatusDemo.py",
//			"-f", "src\\test\\java\\usace\\rowcps\\headless\\GateFlowCalc2.py",
//			"-f", "src\\test\\java\\usace\\rowcps\\headless\\PoolPercentCalc.py",
//			"-f", "src\\test\\java\\usace\\rowcps\\headless\\InflowCalcClone.py",
//			"-f", "src\\test\\java\\usace\\rowcps\\headless\\InflowCalcZeroNegative.py",
		};

		RegiCLI.main(args);
	}
}
