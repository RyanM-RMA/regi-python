package usace.rowcps.headless.calculator.inflow;

import java.util.Date;

/**
 *
 * @author ryan
 */
public interface ScriptableInflow {

    void autoAdjust(String officeId, String locationStr, Date startDate);

    void autoAdjust(String officeId, String locationStr, Date startDate, boolean useLimits, boolean freezeRain);

    void cloneInflows(String officeId, String locationStr, Date startDate);

    void zeroNegatives(String officeId, String locationStr, Date startDate);

    void balanceAll(String officeId, String locationStr, Date startDate);

}
