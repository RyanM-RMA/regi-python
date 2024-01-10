package usace.rowcps.headless.tests;

/**
 * I couldn't get this into a python file...it kept saying it couldn't find the module. I think it's because the
 * evaluator only takes in one file?
 *
 * Anyway, this is intended to provide the global test information for all of the python scripts. I set this up for the
 * SWT office and locations. I've also updated these tests to use an output file location that we can get all our files
 * to so it's not so scattered.
 *
 * @author Ryan A. Miles (ryanm@rmanet.com)
 */
public final class TestVariables
{

	//Office ID for current tests.
	public static final String OFFICE_ID = "SWT";
	//These are intended to be unique, please don't use the same variables.
	public static final String GATE_LOCATION = "FGIB";
	public static final String INFLOW_LOCATION = "EUFA";
	public static final String POOL_LOCATION = "ARBU";
	public static final String LOCATION_4 = "ALTU";
	public static final String[] ALL_PROJECTS = new String[]
	{
		GATE_LOCATION, INFLOW_LOCATION, POOL_LOCATION, LOCATION_4
	};
	public static final String STREAM_GAGE_LOCATION = "RIPL";
	public static final String HEADLESS_FILE_LOCATION = "C:\\Temp\\Headless\\";

	public static void init()
	{

	}

	private TestVariables()
	{
		throw new AssertionError("Don't instantiate this class");
	}
}
