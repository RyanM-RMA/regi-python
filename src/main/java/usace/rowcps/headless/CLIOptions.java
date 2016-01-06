package usace.rowcps.headless;

import hec.lang.PasswordFileEntry;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.Option;

/**
 *
 * @author ryan
 */
public class CLIOptions
{
	private static final Logger logger = Logger.getLogger(CLIOptions.class.getName());
	//public String rowcpsTimezone;
	//public String oracleUrl;
	//public String oracleUser;
	//public String oraclePassword;
	//private Map<String, String> properties = new HashMap<String, String>();
	Properties props = new Properties();

	public final static String URL = "oracle.url";
	public final static String USER = "oracle.user";
	public final static String PASSWORD = "oracle.password";
	public final static String OFFICEID = "oracle.officeId";

	public final static String TIMEZONE = "rowcps.timezone";
	public final static String PROJ_DIR = "rowcps.projectDir";
	public final static String PROJ_NAME = "rowcps.projectName";

	public final static String HEC_PASSWD_FILE = "hec.passwd";

	@Option(name = "-D", metaVar = "<property>=<value>", usage = "use value for given property")
	private void setProperty(final String property) throws CmdLineException
	{
		String[] arr = property.split("=");
		setProperty(arr);
	}

	public void setProperty(String[] arr) throws CmdLineException
	{
		if (arr.length != 2) {
			throw new CmdLineException("Properties must be specified in the form:" +
				"<property>=<value>");
		}
		props.setProperty(arr[0], arr[1]);
		//properties.put(arr[0], arr[1]);
	}

	public Object getProperty(String key)
	{
		return props.get(key);
	}

	public CLIOptions()
	{
	}

	/**
	 * @return the rowcpsTimezone
	 */
	public String getRowcpsTimezone()
	{
		return props.getProperty(TIMEZONE);
		//return rowcpsTimezone;
	}

	public File getRowcpsProjectDir()
	{
		File retval = null;

		String path = props.getProperty(PROJ_DIR);
		if(path != null){
			retval = new File(path);
		}

		return retval;
	}

	@Option(name = "-D" + PROJ_DIR, metaVar = "<file>", usage = "directory containing Regi project")
	public void setRowcpsProjectDir(String filepath)
	{
		props.setProperty(PROJ_DIR, filepath);
	}

	@Option(name = "-D" + HEC_PASSWD_FILE, metaVar = "<file>", usage = "directory containing Regi project")
	public void setHecPasswordFilepath(String filepath)
	{
		props.setProperty(HEC_PASSWD_FILE, filepath);
	}

	public String getHecPasswordFilepath(){
		return props.getProperty(HEC_PASSWD_FILE);
	}

	public PasswordFileEntry getHecPasswordFileEntry()
	{
		PasswordFileEntry retval = null;

		// String office = System.getProperty("cwms.dbi.OfficeId");
		String dburl = getOracleUrl();
		if (dburl != null && !dburl.isEmpty()) {
			String instance = dburl;

			int idx = dburl.indexOf("@");
			if (idx != -1) {
				instance = dburl.substring(idx + 1);
			}

			hec.io.PasswordFile passwordFile = null;
			try {
				String filePath = getHecPasswordFilepath();
				passwordFile = new hec.io.PasswordFile(filePath, false);
				retval = passwordFile.getEntry(instance);

				if (retval == null) {
					/*
					 * System.out.println(
					 * "getConnectionInfo: Failed to find Password Entry for instance "
					 * + instance);
					 */
					logger.severe("getConnectionInfo: Failed to find Password Entry for instance " + instance);

				}
//				// _connectionInfo = new
//				// ConnectionInfo(office,dburl,entry.getUserName(),entry.getPassword());
//				_connectionLoginInfo = new ConnectionLoginInfoImpl(dburl, entry.getUserName(), entry.getPassword(),
//					getOfficeId());
			} catch (java.io.IOException ioe) {
				/*
				 * System.out.println(
				 * "getConnectionInfo: Error reading password file " + ioe);
				 */
				logger.severe("getConnectionInfo: Error reading password file " + ioe);

			} finally {
				if (passwordFile != null) {
					passwordFile.close();
				}
			}
		}

		return retval;
	}


	public String getRowcpsProjectName()
	{
		return props.getProperty(PROJ_NAME);
	}

	@Option(name = "-D" + PROJ_NAME, usage = "name of Regi project")
	public void setRowcpsProjectName(String name)
	{
		props.setProperty(PROJ_NAME, name);
	}

	public String getOracleOfficeId()
	{
		return props.getProperty(OFFICEID);
	}

	@Option(name = "-D" + OFFICEID, usage = "office id")
	public void setOracleOfficeId(String id)
	{
		props.setProperty(OFFICEID, id);
	}

	/**
	 * @param rowcpsTimezone the rowcpsTimezone to set
	 */
	@Option(name = "-D" + TIMEZONE)
	public void setRowcpsTimezone(String rowcpsTimezone) throws CmdLineException
	{
		setProperty(new String[]{TIMEZONE, rowcpsTimezone});
		//this.rowcpsTimezone = rowcpsTimezone;
	}

	/**
	 * @return the oracleUrl
	 */
	public String getOracleUrl()
	{
		return props.getProperty(URL);
		//return oracleUrl;
	}

	/**
	 * @param oracleUrl the oracleUrl to set
	 */
	@Option(name = "-D" + URL)
	public void setOracleUrl(String oracleUrl) throws CmdLineException
	{
		//this.oracleUrl = oracleUrl;
		setProperty(new String[]{URL, oracleUrl});
	}

	/**
	 * @return the oracleUser
	 */
	public String getOracleUser()
	{
		String user = props.getProperty(USER);

		if (user == null) {

			PasswordFileEntry entry = getHecPasswordFileEntry();
			if (entry != null) {
				user = entry.getUserName();
			}

		}
		return user;
		//return oracleUser;
	}

	/**
	 * @param oracleUser the oracleUser to set
	 */
	@Option(name = "-D" + USER)
	public void setOracleUser(String oracleUser) throws CmdLineException
	{
		//this.oracleUser = oracleUser;
		setProperty(new String[]{USER, oracleUser});
	}

	/**
	 * @return the oraclePassword
	 */
	public char[] getOraclePassword()
	{
		char[] pass = null;
		//return oraclePassword;
		String passStr = props.getProperty(PASSWORD);
		if (passStr != null) {
			pass = passStr.toCharArray();
		} else {
			PasswordFileEntry entry = getHecPasswordFileEntry();
			if(entry != null){
				pass = entry.getPassword().toCharArray();
			}
		}

		return pass;
	}

	/**
	 * @param oraclePassword the oraclePassword to set
	 */
	@Option(name = "-D" + PASSWORD)
	public void setOraclePassword(String oraclePassword) throws CmdLineException
	{
		setProperty(new String[]{PASSWORD, oraclePassword});
		//this.oraclePassword = oraclePassword;
	}

	@Option(name = "-p", aliases = {"-properties"}, metaVar = "<file>",
		usage = "import properties from given file")
	public void importProperties(File file)
	{
		if (file != null && file.exists()) {
			Properties fileProps = new Properties();

			try (BufferedReader br = new BufferedReader(new FileReader(file))) {
				fileProps.load(br);

				Set<Map.Entry<Object, Object>> entrySet = fileProps.entrySet();
				for (Map.Entry<Object, Object> entry : entrySet) {
					Object keyObj = entry.getKey();
					Object valueObj = entry.getValue();

					if (keyObj != null && valueObj != null) {
						props.put(keyObj, valueObj);
					}
				}
			} catch (FileNotFoundException ex) {
				Logger.getLogger(CLIOptions.class.getName()).log(Level.SEVERE, null, ex);
			} catch (IOException ex) {
				Logger.getLogger(CLIOptions.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
	}

	@Option(name = "-f", aliases = {"-file"}, metaVar = "<file>",
		usage = "script file to execute")
	public void setScriptFile(File file)
	{
		props.setProperty("script", file.getAbsolutePath());
	}

	public String getScriptPath()
	{
		return props.getProperty("script");
	}

	public File getScriptFile()
	{
		File retval = null;
		String scriptPath = getScriptPath();
		if (scriptPath != null && !scriptPath.isEmpty()) {
			File afile = new File(scriptPath);
			if (afile.exists()) {
				retval = afile;
			}
		}
		return retval;
	}

}
