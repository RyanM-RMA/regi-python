package usace.rowcps.headless.calculator.status;

import java.util.HashMap;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Very simple templating system which replaces %keyword% with supplied values.
 * Supported patterns look like: "J:\\temp\\headless\\%office_id%_%basin_id%_%location_id%_%chart_template_id%_%date%.png"
 * @author ryan
 */
public class SimpleTemplateEngine {
	private static final Logger logger = Logger.getLogger(SimpleTemplateEngine.class.getName());

	protected HashMap<String, String> replacements = new HashMap<>();
	public SimpleTemplateEngine() {
	}

	static Pattern pattern = Pattern.compile("%\\w+%");  // This defines the %word% pattern we will look for.
	
	public void addPattern( String pattern, String value){
		if(pattern.contains("%")){
			logger.warning("Supplied pattern contained '%' character, this is unlikely to work correctly.");
		}
		replacements.put("%" + pattern + "%", value);
	}
	
	public String makeReplacements(String text ){
		return makeReplacements(text, replacements);
	}
	
	public static String makeReplacements(String text, HashMap<String, String> replacements) {
		Matcher matcher = pattern.matcher(text);

		StringBuffer buffer = new StringBuffer();

		while (matcher.find()) {
			String found = matcher.group();
			String replacement = replacements.get(found);
			if (replacement != null) {
				matcher.appendReplacement(buffer, "");
				buffer.append(replacement);
			} else {
				logger.info("Unrecognized replacement pattern:" + found + ". "
						+ " Recognized replacement patterns are:" + replacements.keySet());
			}
		}
		matcher.appendTail(buffer);

		return buffer.toString();
	}
			
}
