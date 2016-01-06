
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
	public String getName();
	public String getDescription();
	public String getUsage();

	public T build(RegiDomain regiDomain, ManagerId managerid);
}
