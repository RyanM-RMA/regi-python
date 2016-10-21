package usace.rowcps.headless;

import java.util.ArrayList;
import java.util.Collection;
import usace.rowcps.headless.calculator.CalcFactoryRegistry;
import usace.rowcps.headless.interfaces.ScriptableCalc;
import usace.rowcps.regi.model.ManagerId;
import usace.rowcps.regi.model.RegiDomain;

/**
 *
 * @author ryan
 */
public class RegiCalcRegistry
{
	RegiDomain regiDomain;
	ManagerId managerId;

	public RegiCalcRegistry(RegiDomain regiDomain, ManagerId managerId)
	{
		this.regiDomain = regiDomain;
		this.managerId = managerId;
	}

	public Collection<ScriptableCalc> getCalculation(double vesion)
	{
		Collection<ScriptableCalc> retval = null;
		CalcFactoryRegistry registry = CalcFactoryRegistry.getRegistry(vesion);

		Collection<ScriptableCalcFactory> factories = registry.getFactories();
		if (factories != null && !factories.isEmpty()) {
			retval = new ArrayList<>();
			for (ScriptableCalcFactory factory : factories) {
				retval.add(factory.build(regiDomain, managerId));
			}

		}
		return retval;
	}

	public ScriptableCalc getCalculation(double version, String name)
	{
		ScriptableCalc retval = null;
		CalcFactoryRegistry registry = CalcFactoryRegistry.getRegistry(version);

		ScriptableCalcFactory factory = registry.getFactory(name);

		if (factory != null) {
			retval = factory.build(regiDomain, managerId);
		}

		return retval;
	}

	public Collection<String> getNames(double version)
	{
		CalcFactoryRegistry registry = CalcFactoryRegistry.getRegistry(version);
		return registry.getNames();
	}
}
