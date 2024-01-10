package usace.rowcps.headless;

import java.util.logging.Level;
import java.util.logging.Logger;
import usace.rowcps.regi.interfaces.progress.IProgressHandle;

/**
 *
 * @author ryan
 */
public class HeadlessHandleImpl implements IProgressHandle
{
	private static final Logger logger = Logger.getLogger(HeadlessHandleImpl.class.getName());
	private String name;
	private boolean _isRunning;

	HeadlessHandleImpl(String string)
	{
		name = string;
	}

	@Override
	public void start()
	{
		logger.log(Level.FINE, "start");
		_isRunning = true;
	}

	@Override
	public void start(int workunits)
	{
		logger.log(Level.FINE, "start");
		_isRunning = true;
	}

	@Override
	public void start(int workunits, long estimate)
	{
		logger.log(Level.FINE, "start");
		_isRunning = true;
	}

	@Override
	public void switchToIndeterminate()
	{
		logger.log(Level.FINE, "switchToIndeterminate");
	}

	@Override
	public void suspend(String message)
	{
		logger.log(Level.FINE, "suspend");
		_isRunning = false;
	}

	@Override
	public void switchToDeterminate(int workunits)
	{
		logger.log(Level.FINE, "switchToDeterminate");
	}

	@Override
	public void switchToDeterminate(int workunits, long estimate)
	{
		logger.log(Level.FINE, "switchToDeterminate");
	}

	@Override
	public void finish()
	{
		logger.log(Level.FINE, "finish");
		_isRunning = false;
	}

	@Override
	public void progress(int workunit)
	{
		logger.log(Level.FINE, "progress");
	}

	@Override
	public void progress(String message)
	{
		logger.log(Level.FINE, "progress");
	}

	@Override
	public void progress(String message, int workunit)
	{
		logger.log(Level.FINE, "progress");
	}

	@Override
	public void setInitialDelay(int millis)
	{
		logger.log(Level.FINE, "setInitialDelay");
	}

	@Override
	public void setDisplayName(String newDisplayName)
	{
		logger.log(Level.FINE, "setDisplayName");
	}
	
	@Override
	public boolean isRunning()
	{
		return _isRunning;
	}

}
