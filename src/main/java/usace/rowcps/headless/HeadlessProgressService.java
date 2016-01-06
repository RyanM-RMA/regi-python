package usace.rowcps.headless;

import rma.services.annotations.ServiceProvider;
import usace.rowcps.regi.interfaces.progress.IProgressHandle;
import usace.rowcps.regi.interfaces.progress.ProgressService;

/**
 *
 * @author ryan
 */
@ServiceProvider(service = ProgressService.class)
public class HeadlessProgressService implements ProgressService
{

	@Override
	public IProgressHandle createHandle(String string)
	{

		IProgressHandle handleImpl = new HeadlessHandleImpl(string);
		return handleImpl;
	}


}
