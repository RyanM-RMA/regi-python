/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package usace.rowcps.headless;

import java.util.logging.Level;
import java.util.logging.Logger;
import wcds.dbi.DbiProperties;

/**
 *
 * @author ryanm
 */
public class LoggingOptions
{
    /**
     * Sets the DbiProperties message level so we're reporting additional DB
     * related messages.
     * 
     * Valid values range from 0-6, values outside of this range act as either
     * 0 or 6, because the messages generally look for &gt;5 and &lt;= 0.
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
}
