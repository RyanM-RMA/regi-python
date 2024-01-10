
package usace.rowcps.headless;

import usace.rowcps.headless.interfaces.ScriptableCalc;
import usace.rowcps.regi.model.ManagerId;
import usace.rowcps.regi.model.RegiDomain;

/**
 *
 * @author ryan
 */
public interface ScriptableCalcFactory<T extends ScriptableCalc>
{
	String getName();
	String getDescription();
	String getUsage();

	T build(RegiDomain regiDomain, ManagerId managerid);
}
