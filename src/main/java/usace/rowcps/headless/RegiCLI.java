package usace.rowcps.headless;

import usace.rowcps.headless.interfaces.ScriptEvaluator;
import hec.db.DbConnectionException;
import hec.db.DbPluginNotFoundException;
import hec.db.InvalidDbConnectionException;
import hec.security.LoginState;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.kohsuke.args4j.CmdLineParser;
import usace.rowcps.regi.factories.RowcpsExecutorService;
import usace.rowcps.regi.model.ManagerId;
import usace.rowcps.regi.model.RegiDomain;
import wcds.dbi.oracle.ui.OracleServerInfo;

/**
 *
 * @author ryan
 */
public class RegiCLI
{
	private static final Logger logger = Logger.getLogger(RegiCLI.class.getName());

	public static void main(String[] args)
	{
//		if (args == null || args.length == 0) {
//
//			args = new String[]{
//				"-Drowcps.timezone=GMT",
//				"-p", "src\\test\\java\\usace\\rowcps\\headless\\credentials.properties",
//////				"-f", "src\\test\\java\\usace\\rowcps\\headless\\hello.py",
//////				"-f", "src\\test\\java\\usace\\rowcps\\headless\\GateFlowCalc2.py",
////					"-f", "src\\test\\java\\usace\\rowcps\\headless\\PoolPercentCalc.py",
//					"-f", "src\\test\\java\\usace\\rowcps\\headless\\InflowCalcClone.py",
////					"-f", "src\\test\\java\\usace\\rowcps\\headless\\InflowCalcZeroNegative.py",
//			};
//		}

		CLIOptions opt = new CLIOptions();
		CmdLineParser parser = new CmdLineParser(opt);

		ManagerId managerId = null;
		RegiDomain regiDomain = null;

		OracleServerInfo osi = null;
		LoginState ls = null;

		try {
			parser.parseArgument(args);

			HeadlessRegiDomainFactory factory = new HeadlessRegiDomainFactory();
			managerId = factory.getManagerId(opt);

			regiDomain = factory.createDomain(opt, managerId);
			if (regiDomain != null) {
				ScriptEvaluator pe = new PythonEvaluator();
				Map<String, Object> vars = new HashMap<>();

				RegiCalcRegistry reg = new RegiCalcRegistry(regiDomain, managerId);
				vars.put("registry", reg);

				File scriptFile = opt.getScriptFile();

				try {
					FileReader fr = new FileReader(scriptFile);
					logger.info("Evaluating script file");
					Object retval = pe.evaluateExpression(fr, vars);

					logger.info("Jython script completed normally, commiting data.");
					regiDomain.commitData(managerId);
					logger.info("RegiDomain committed data.");
				} catch (Exception ex) {
					Logger.getLogger(RegiCLI.class.getName()).log(Level.SEVERE, null, ex);
				}

				logger.info("RegiDomain closing.");
				regiDomain.closing();
			}

		} catch (DbConnectionException | DbPluginNotFoundException | InvalidDbConnectionException ex) {
			Logger.getLogger(RegiCLI.class.getName()).log(Level.SEVERE, null, ex);
			return;
		} catch (Exception  e) {
			System.err.println(e.getMessage());
			System.err.println("java -jar myprogram.jar [options...] arguments...");
			parser.printUsage(System.err);
			return;
		}
		RowcpsExecutorService res = RowcpsExecutorService.getInstance();

//		if(!res.isShutdown()){
//			logger.info("RowcpsExecutorService was not shutdown.");
//			res.shutdown();
//			try {
//				res.awaitTermination(5, TimeUnit.SECONDS);
//			} catch (InterruptedException ex) {
//				Logger.getLogger(RegiCLI.class.getName()).log(Level.SEVERE, null, ex);
//				Thread.currentThread().interrupt();
//			}
//		}

		logger.info("Exitting.");
		System.exit(0);
	}
}
