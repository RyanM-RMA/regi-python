package usace.rowcps.headless.calculator.gatesettings;

import java.util.Date;

/**
 *
 * @author ryan
 */
interface ScriptableGateSettings
{

	void createGateSettings(String officeId, String locationStr, Date startDate, Date end) throws Exception;

	void createGateSettingsOutlet(String officeId, String locationStr, Date startDate, Date end, String outletId) throws Exception;

	void createGateSettingsGroup(String officeId, String locationStr, Date startDate, Date end, String groupId) throws Exception;
}
