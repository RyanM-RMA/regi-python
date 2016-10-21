package usace.rowcps.headless;

import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import usace.rowcps.headless.calculator.CalcFactoryRegistry;
import usace.rowcps.headless.interfaces.ScriptableCalc;
import usace.rowcps.regi.model.ManagerId;
import usace.rowcps.regi.model.RegiDomain;
import wcds.dbi.DbiProperties;

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
    public void setDbMessageLevel(int messageLevel)
    {
        DbiProperties.MESSAGE_LEVEL = messageLevel;
        Logger.getLogger(RegiCalcRegistry.class.getName()).log(Level.FINE, "Setting Db Message Level to: {0}", messageLevel);
    }

}
