package usace.rowcps.headless.calculator.poolpercent;

import rma.services.annotations.ServiceProvider;
import usace.rowcps.headless.ScriptableCalcFactory;
import usace.rowcps.headless.interfaces.ScriptableCalc;
import usace.rowcps.regi.model.ManagerId;
import usace.rowcps.regi.model.RegiDomain;

/**
 *
 * @author ryan
 */
@ServiceProvider(service = ScriptableCalcFactory.class)
public class PoolPercentCalcFactory implements ScriptableCalcFactory
{

	@Override
	public String getName()
	{
		return "Pool Percent";
	}

	@Override
	public String getDescription()
	{
		return "Recalculates Pool Percentages at a Location";
	}

	@Override
	public String getUsage()
	{
		return "";
	}

	@Override
	public ScriptableCalc build(RegiDomain regiDomain, ManagerId managerid)
	{
		return new ScriptablePoolPercentImpl(regiDomain, managerid);
	}

}