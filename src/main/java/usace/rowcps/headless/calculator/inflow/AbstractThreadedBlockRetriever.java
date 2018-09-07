package usace.rowcps.headless.calculator.inflow;

import java.beans.PropertyChangeListener;
import usace.rowcps.regi.event.IThreadedBlockRetriever;

/**
 *
 * @author ryan
 */
public abstract class AbstractThreadedBlockRetriever implements IThreadedBlockRetriever
{
	private boolean allowTailFetch = false;
	private boolean allowHeadFetch = false;
	

	@Override
	public void addWeakPropertyChangeListener(PropertyChangeListener newListener)
	{

	}

	@Override
	public void removeWeakPropertyChangeListener(PropertyChangeListener newListener)
	{

	}

	@Override
	public boolean isAllowTailFetch()
	{
		return allowTailFetch;
	}

	@Override
	public void setAllowTailFetch(boolean allowTailFetch)
	{
		this.allowTailFetch = allowTailFetch;
	}

	@Override
	public boolean isAllowHeadFetch()
	{
		return allowHeadFetch;
	}

	@Override
	public void setAllowHeadFetch(boolean allowHeadFetch)
	{
		this.allowHeadFetch = allowHeadFetch;
	}

	@Override
	public void asyncTailCacheFetchStarted()
	{

	}

	@Override
	public void asyncHeadCacheFetchStarted()
	{

	}

	@Override
	public void asyncTailCacheFetchCompleted()
	{

	}

	@Override
	public void asyncHeadCacheFetchCompleted()
	{

	}
}
