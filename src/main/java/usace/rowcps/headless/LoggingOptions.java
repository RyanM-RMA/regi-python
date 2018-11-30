/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package usace.rowcps.headless;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import usace.metrics.services.MetricsInitializationException;
import usace.metrics.services.MetricsServiceProvider;
import usace.rowcps.regi.factories.RegiDomainFactory;
import usace.rowcps.regi.preferences.RegiPreferences;
import wcds.dbi.DbiProperties;

/**
 *
 * @author ryanm
 */
public class LoggingOptions
{

    private static boolean _metricsInitialized = false;

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
        Logger.getLogger(LoggingOptions.class.getName()).log(Level.FINE, "Setting Db Message Level to: {0}", messageLevel);
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
        initMetrics();

        MetricsServiceProvider.setMetricsEnabled(enabled);

        if (enabled)
        {
            Logger.getLogger(LoggingOptions.class.getName()).log(Level.INFO, "Metrics enabled, files can be found at {0}", MetricsServiceProvider.getFileLocation());
        }
    }

    private static void initMetrics()
    {
        if (!_metricsInitialized)
        {
            Date now = new Date();
            DateFormat format = new SimpleDateFormat("yyyy-MM-dd kkmm-ss");

            try
            {
				MetricsServiceProvider.init(RegiPreferences.getClientNode().node("Metrics"), format.format(now), RegiDomainFactory.getRegiBaseDir());
                _metricsInitialized = true;
            }
            catch (MetricsInitializationException ex)
            {
                //It's been initialized, let's not try again.
            }
        }
    }
}
