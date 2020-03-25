package usace.rowcps.headless;

import hec.db.DbConnectionException;
import hec.db.DbIoException;
import hec.db.DbPluginNotFoundException;
import hec.db.InvalidDbConnectionException;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Scope;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.kohsuke.args4j.CmdLineException;
import usace.rowcps.headless.interfaces.ScriptEvaluator;
import usace.rowcps.regi.factories.RowcpsExecutorService;
import usace.rowcps.regi.model.ManagerId;
import usace.rowcps.regi.model.RegiDomain;

public class RegiCLI
{

	private static final Logger LOGGER = Logger.getLogger(RegiCLI.class.getName());

	public static void main(String[] args)
	{
		Span rootSpan = GlobalOpenTelemetry.getTracer("regi-headless")
			.spanBuilder("runHeadless")
			.startSpan();
		try(Scope scope = rootSpan.makeCurrent())
		{
			runHeadlessTest(args);
		}
		catch (DbConnectionException | DbPluginNotFoundException | IOException | RuntimeException ex)
		{
			rootSpan.recordException(ex);
			rootSpan.setStatus(StatusCode.ERROR);
			LOGGER.log(Level.SEVERE, "Headless error connecting to database.", ex);
			System.exit(-1);
			return;
		}

		LOGGER.info("Exiting.");
		System.exit(0);
	}
	
	/**
	 * Used by TestHeadless unit test class to run headless without calling System.exit(0)
	 * 
	 * @param unused
	 * @throws DbConnectionException
	 * @throws InvalidDbConnectionException
	 * @throws CmdLineException
	 * @throws DbPluginNotFoundException 
	 */
	static void runHeadlessTest(String[] unused)
        throws DbConnectionException, DbPluginNotFoundException,
        IOException {
		
		HeadlessRegiDomainFactory factory = new HeadlessRegiDomainFactory();
		RegiDomain regiDomain = factory.createDomain();
		
		if (regiDomain != null)
		{
			ScriptEvaluator pe = new PythonEvaluator();
			Map<String, Object> vars = new HashMap<>();

			ManagerId managerId = factory.getManagerId();
			RegiCalcRegistry reg = new RegiCalcRegistry(regiDomain, managerId);
			vars.put("registry", reg);
			
			File scriptFile = new File(System.getenv("SCRIPT"));
			
			try
			{
				FileReader fr = new FileReader(scriptFile);
				LOGGER.info("Evaluating script file");
				Object retval = pe.evaluateExpression(fr, vars);
				
				LOGGER.info("Jython script completed normally, commiting data.");
				regiDomain.commitData(managerId);
				LOGGER.info("RegiDomain committed data.");
			}
			catch (DbConnectionException | DbIoException | FileNotFoundException ex)
			{
				LOGGER.log(Level.SEVERE, "Exception occurred while evaluating Jython file:" + System.lineSeparator() + scriptFile, ex);
			}
			finally
			{
				LOGGER.info("RegiDomain closing.");
				shutdownRowcpsAccessFactory(managerId);
				regiDomain.closing();
			}
		}
	}

	private static void shutdownRowcpsAccessFactory(ManagerId managerId)
	{
		LOGGER.log(Level.INFO, "Shutting down RowcpsExecutorService for {0}", managerId);
		RowcpsExecutorService res = RowcpsExecutorService.getInstance(managerId);

		res.shutdown();  // signal shutdown - this will stop accepting new jobs and allow existing jobs to complete.
		boolean exitted = false;
		try
		{
			// We are willing to wait a little to achieve a clean shutdown.
			exitted = res.awaitTermination(3000, TimeUnit.MILLISECONDS);

		}
		catch (InterruptedException ie)
		{
			Thread.currentThread().interrupt();
		}
		if (!exitted)
		{
			// Some of the running tasks didn't exit in the time we were willing to wait.
			List<Runnable> wereWaiting = res.shutdownNow();  // This will interrupt them if they support interruption.
		}
		LOGGER.log(Level.INFO, "RowcpsExecutorService for {0} shutdown complete.", managerId);
	}
}
