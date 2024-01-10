package usace.rowcps.headless.calculator.flowgroup;

import java.util.Date;

/**
 *
 * @author ryan
 */
public interface ScriptableGateFlowCalc
{

	void computeAll(String officeId, String locationId, Date start, Date end);
	void computeAll(String officeId, String locationId, long startTime, long endTime);

	void computeAll(String officeId, String[] locationIds, Date start, Date end);
	void computeAll(String officeId, String[] locationIds, long startTime, long endTime);

	void computeFlowGroup(String officeId, String locationId, Date start, Date end, String groupId);
	void computeFlowGroup(String officeId, String locationId, long startTime, long endTime, String groupId);

	void computeFlowGroup(String officeId, String[] locationIds, Date start, Date end, String groupId);
	void computeFlowGroup(String officeId, String[] locationIds, long startTime, long endTime, String groupId);

}
