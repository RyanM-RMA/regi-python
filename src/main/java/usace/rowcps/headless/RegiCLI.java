package usace.rowcps.headless;

import usace.rowcps.headless.interfaces.ScriptEvaluator;
import hec.db.DbConnectionException;
import hec.db.DbPluginNotFoundException;
import hec.db.InvalidDbConnectionException;
import hec.security.LoginState;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
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
//				"-Drowcps.timezone=America/Chicago",
//				"-p", "src\\test\\java\\usace\\rowcps\\headless\\credentials.properties",
////                                "-f", "src\\test\\java\\usace\\rowcps\\headless\\BasinPie.py",
//				"-f", "src\\test\\java\\usace\\rowcps\\headless\\StatusDemo.py",
////////				"-f", "src\\test\\java\\usace\\rowcps\\headless\\GateFlowCalc2.py",
//////					"-f", "src\\test\\java\\usace\\rowcps\\headless\\PoolPercentCalc.py",
////					"-f", "src\\test\\java\\usace\\rowcps\\headless\\InflowCalcClone.py",
//////					"-f", "src\\test\\java\\usace\\rowcps\\headless\\InflowCalcZeroNegative.py",
//			};
//		}

		CLIOptions opt = new CLIOptions(System.getProperties());
		CmdLineParser parser = new CmdLineParser(opt);

		ManagerId managerId = null;
		RegiDomain regiDomain = null;

		OracleServerInfo osi = null;
		LoginState ls = null;

		try {
			parser.parseArgument(args);
			System.setProperties(opt.getProperties());

			HeadlessRegiDomainFactory factory = new HeadlessRegiDomainFactory();
			managerId = factory.getManagerId(opt);

			regiDomain = factory.createDomain(opt, managerId);
			if (regiDomain !=
                            null) {
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
				shutdownRowcpsAccessFactory(managerId);
				regiDomain.closing();
			}

		} catch (DbConnectionException | DbPluginNotFoundException | InvalidDbConnectionException ex) {
			Logger.getLogger(RegiCLI.class.getName()).log(Level.SEVERE,"Headless error connecting to database.", ex);
			return;
		} catch (Exception  e) {
                        Logger.getLogger(RegiCLI.class.getName()).log(Level.SEVERE, "Error running headless", e);
			System.err.println("java -jar myprogram.jar [options...] arguments...");
			parser.printUsage(System.err);
			return;
		}

		logger.info("Exitting.");
		System.exit(0);
	}

	private static void shutdownRowcpsAccessFactory(ManagerId managerId)
	{
		logger.info("Shutting down RowcpsExecutorService for " + managerId );
		RowcpsExecutorService res = RowcpsExecutorService.getInstance(managerId);

		res.shutdown();  // signal shutdown - this will stop accepting new jobs and allow existing jobs to complete.
		boolean exitted = false;
		try{
			// We are willing to wait a little to achieve a clean shutdown.
			exitted = res.awaitTermination(3000, TimeUnit.MILLISECONDS);

		} catch (InterruptedException ie){
			Thread.currentThread().interrupt();
		}
		if(!exitted){
			// Some of the running tasks didn't exit in the time we were willing to wait.
			List<Runnable> wereWaiting = res.shutdownNow();  // This will interrupt them if they support interruption.
		}
		logger.info("RowcpsExecutorService for " + managerId + " shutdown complete.");
	}
}
