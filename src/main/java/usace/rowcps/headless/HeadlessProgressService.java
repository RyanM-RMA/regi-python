package usace.rowcps.headless;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
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
	private static final Logger logger = Logger.getLogger(HeadlessProgressService.class.getName());
	
	private List<IProgressHandle> created = new ArrayList<IProgressHandle>();
	private List<IProgressHandle> finished = new ArrayList<IProgressHandle>();

	@Override
	public IProgressHandle createHandle(String string)
	{
		logger.info("Creating progress handle for " + string);
		IProgressHandle handleImpl = new HeadlessHandleImpl(string){
			@Override
			public void finish() {
				super.finish();
				finished.add(this);
			}
			
		};
		created.add(handleImpl);
		return handleImpl;
	}


}
