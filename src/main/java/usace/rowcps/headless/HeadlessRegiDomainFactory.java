package usace.rowcps.headless;

import com.rma.io.FileManager;
import com.rma.io.FileManagerImpl;
import com.rma.io.RmaFile;
import com.rma.model.Manager;
import com.rma.model.Project;
import hec.db.DbConnectionException;
import hec.db.DbPluginNotFoundException;
import hec.db.InvalidDbConnectionException;
import hec.io.Identifier;
import java.io.File;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import rma.services.ServiceLookup;
import rma.services.tz.TimeZoneDisplayService;
import usace.rowcps.regi.factories.RegiDomainFactory;
import usace.rowcps.regi.interfaces.model.ManagerIdProvider;
import usace.rowcps.regi.model.DatabaseConnectionManager;
import usace.rowcps.regi.model.ManagerId;
import usace.rowcps.regi.executor.ManagerIdType;

import usace.rowcps.regi.model.RegiDomain;

/**
 *
 * @author ryan
 */
public class HeadlessRegiDomainFactory
{

	private static final Logger logger = Logger.getLogger(HeadlessRegiDomainFactory.class.getName());

	public void setPluginsDirFromClasspath()
	{
		String cp = System.getProperties().getProperty("java.class.path");
		String[] split = cp.split(File.pathSeparator);
		final String dbiClientjar = "dbiClient.jar";
		for (String cpentry : split) {
			if (cpentry.endsWith(dbiClientjar)) {
				String pluginDir = cpentry.split(dbiClientjar)[0];
				logger.log(Level.INFO, "Setting plugin dir to: {0}", pluginDir);
				System.setProperty("PLUGINS", pluginDir);
			}
		}
	}

	public RegiDomain createDomain(CLIOptions options, ManagerId managerId) throws DbConnectionException,
		DbPluginNotFoundException, InvalidDbConnectionException
	{
		RegiDomain regiDomain = null;

		setPluginsDirFromClasspath();
		File rowcpsPojectDir = options.getRowcpsProjectDir();

		String rowcpsProjectName = options.getRowcpsProjectName();

		File projectDir = new File(rowcpsPojectDir, rowcpsProjectName);

		if (projectDir == null) {
			String missingProjDirMessage
				= "A Rowcps Project Dir is required and must be specified on the command line or in a properties file.";
			throw new IllegalArgumentException(missingProjDirMessage);
		} else {
			if (!projectDir.exists()) {
				// If we are being run headlessly I'm not sure how much hand-holding and sanity checking we have to do.
				projectDir.mkdirs();
				if (!projectDir.exists()) {
					throw new IllegalArgumentException("The directory " + projectDir.getAbsolutePath() +
						" did not exist and could not be created.");
				}
			}

			String testProjDir = projectDir.getAbsolutePath();
			FileManager fileManager = FileManagerImpl.getFileManager();
			final String projectFilePath = testProjDir + "/" + options.getRowcpsProjectName() + ".prj";

			RmaFile prjFile;
			if (!fileManager.fileExists(projectFilePath)) {
				final Identifier identifier = new Identifier(projectFilePath);
				Identifier prjId = fileManager.createFile(identifier);
				prjFile = fileManager.getFile(prjId.getPath());
			} else {
				prjFile = fileManager.getFile(projectFilePath);
			}

			logger.log(Level.INFO, "Temp project file: " + prjFile.getAbsolutePath());

			File projReportsDir = new File(projectDir, "reports");
			File projXmlDir = new File(projectDir, "xml");
			projReportsDir.mkdir();
			projXmlDir.mkdir();

			String name = null;
			String description = null;
			regiDomain = new RegiDomainFactory().createProject(name, description, prjFile);

			regiDomain.loadProjectFile();

			DatabaseConnectionManager connectionManager = (DatabaseConnectionManager) regiDomain.getManager(
				RegiDomain.DOMAIN_CONNECTION_MANAGER, DatabaseConnectionManager.class);

			if (connectionManager == null) {
				connectionManager = regiDomain.buildDatabaseConnectionManager();
			}

			//conigure the database connection.
			String dbUrl = options.getOracleUrl();
			String username = options.getOracleUser();
			char[] password = options.getOraclePassword();
			String tzId = options.getRowcpsTimezone();
			String officeId = options.getOracleOfficeId();

			connectionManager.setTimeZoneId(tzId);
			connectionManager.setDbUrl(dbUrl);
			connectionManager.setUsername(username);
			connectionManager.setPass(password);
			connectionManager.setUserOfficeId(officeId);
			connectionManager.saveData();
			
			TimeZoneDisplayService tsDS = ServiceLookup.getTimeZoneDisplayService();
			tsDS.setTimeZone(connectionManager.getTimeZone());

			regiDomain.connect();
			List<Manager> managerList = regiDomain.getManagerList();

			regiDomain.saveProject();
			Project.setCurrentProject(regiDomain);
		}
		return regiDomain;
	}

	public ManagerId getManagerId(CLIOptions opt)
	{
		ManagerId retval = idProvider.getManagerId();

		return retval;
	}

	private ManagerIdProvider idProvider = buildNewProvider();

	private static ManagerIdProvider buildNewProvider()
	{
		return new ManagerIdProvider()
		{
			private final ManagerId _managerId = ManagerId.buildNewManagerId("Headless_" + System.currentTimeMillis(), ManagerIdType.HEADLESS);

			@Override
			public ManagerId getManagerId()
			{
				return _managerId;
			}

		};
	}

}
