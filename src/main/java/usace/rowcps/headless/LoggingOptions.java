/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package usace.rowcps.headless;

import java.util.logging.Level;
import java.util.logging.Logger;
import usace.rowcps.headless.metrics.RegiHeadlessMetricsServiceProvider;
import usace.rowcps.metrics.RegiMetricsService;
import wcds.dbi.DbiProperties;

/**
 *
 * @author ryanm
 */
public class LoggingOptions
{

	private static final Logger LOGGER = Logger.getLogger(LoggingOptions.class.getName());
	
	private LoggingOptions()
	{
		throw new AssertionError("Instantiation of a utility class is not allowed.");
	}

    /**
     * Sets the DbiProperties message level so we're reporting additional DB
     * related messages.
     *
     * Valid values range from 0-6, values outside of this range act as either 0
     * or 6, because the messages generally look for &gt;5 and &lt;= 0.
     *
     * This should probably be put somewhere other than the RegiCalcRegistry,
     * but for now this works.
     *
     * @param messageLevel
     */
    public static void setDbMessageLevel(int messageLevel)
    {
        DbiProperties.MESSAGE_LEVEL = messageLevel;
        LOGGER.log(Level.FINE, "Setting Db Message Level to: {0}", messageLevel);
    }

    /*
     * Enables or disables the storage of REGI's Metric data pertaining to the
     * performance of the application. This is incredibly helpful for
     * identifying issues where the application takes an excessive amount of
     * time to operate.
     *
     * Metrics also log the location of the files as an INFO message if they are
     * enabled. By default, Metrics are disabled.
     */
    public static void setMetricsEnabled(boolean enabled)
    {
        RegiHeadlessMetricsServiceProvider.setMetricsEnabled(enabled);

        if (enabled)
        {
            LOGGER.log(Level.INFO, "Metrics enabled, files can be found at {0}",
					RegiMetricsService.getMetricsConfig().getPreferredFileLocation());
        }
    }
}
