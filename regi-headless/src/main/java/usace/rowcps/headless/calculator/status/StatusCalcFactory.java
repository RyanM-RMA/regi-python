package usace.rowcps.headless.calculator.status;

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
public class StatusCalcFactory implements ScriptableCalcFactory
{

	@Override
	public String getName()
	{
		return "Status";
	}

	@Override
	public String getDescription()
	{
		return "Generates Status Graphic images";
	}

	@Override
	public String getUsage()
	{
		return "";
	}

	@Override
	public ScriptableCalc build(RegiDomain regiDomain, ManagerId managerid)
	{
		return new ScriptableStatusGraphicImpl(regiDomain, managerid);
	}

}