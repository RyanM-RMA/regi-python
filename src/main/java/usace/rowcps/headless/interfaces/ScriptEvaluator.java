package usace.rowcps.headless.interfaces;

import java.io.Reader;
import java.util.Map;

/**
 *
 * @author ryan
 */
public interface ScriptEvaluator
{
	Object evaluateExpression(Reader reader, Map<String, Object> variables);
}
