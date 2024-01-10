package usace.rowcps.headless.calculator;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import usace.rowcps.headless.ScriptableCalcFactory;
import usace.rowcps.headless.SimpleFactoryRegistry;

/**
 *
 * @author ryan
 */
public abstract class CalcFactoryRegistry
{

	private static final Logger logger = Logger.getLogger(CalcFactoryRegistry.class.getName());
	static Map<Double, CalcFactoryRegistry> registryMap = new HashMap<Double, CalcFactoryRegistry>();
	public static double DEFAULT_VERSION = 1.0;

	public static CalcFactoryRegistry getRegistry(double version)
	{
		CalcFactoryRegistry registry = registryMap.get(version);
		if (registry == null) {
			registry = buildRegistry(version);
			if (registry != null) {
				registryMap.put(version, registry);
			}

		}
		return registry;
	}

	private static CalcFactoryRegistry buildRegistry(double version)
	{
		CalcFactoryRegistry registry = null;
		if (1.0d == version) {
			registry = new SimpleFactoryRegistry();
		}
		return registry;
	}

	abstract public Collection<ScriptableCalcFactory> getFactories();

	abstract public ScriptableCalcFactory getFactory(String name);

	abstract public Collection<String> getNames();

	

}
