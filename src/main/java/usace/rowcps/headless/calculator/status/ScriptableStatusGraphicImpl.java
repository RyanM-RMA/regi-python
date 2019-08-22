package usace.rowcps.headless.calculator.status;

import com.rma.ui.pinnable.PinnableComponentGlassPane;
import com.rma.ui.pinnable.PinnableComponentGlassPaneFactory;
import com.rma.ui.pinnable.PinnableContainer;
import hec.data.location.AssignedLocation;
import hec.data.location.Location;
import hec.data.location.LocationGroup;
import hec.data.location.LocationTemplate;
import hec.data.project.IProject;
import hec.db.DbConnectionException;
import hec.db.DbIoException;
import hec.heclib.util.HecTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import usace.rowcps.data.maptemplate.IMapTemplate;
import usace.rowcps.data.maptemplate.streamplot.StreamGageGraphicOptionData;
import usace.rowcps.decisionsupport.ui.mappanel.template.impl.TimeInfoSource;
import usace.rowcps.decisionsupport.ui.streamplot.StreamData;
import usace.rowcps.decisionsupport.ui.streamplot.StreamPlotPanel;
import usace.rowcps.headless.interfaces.ScriptableCalc;
import usace.rowcps.regi.model.AtLocationManager;
import usace.rowcps.regi.model.AtMapTemplateManager;
import usace.rowcps.regi.model.CacheUsage;
import usace.rowcps.regi.model.ManagerId;
import usace.rowcps.regi.model.RegiDomain;
import hec.map.geoui.interp.TimeInfo;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.SortedSet;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import javax.imageio.IIOImage;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.swing.JComponent;
import javax.swing.JLayer;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import rma.services.GlobalServiceLoader;
import rma.services.GlobalServiceLoaderDelegate;
import usace.metrics.services.Metrics;
import usace.metrics.services.MetricsServiceProvider;
import usace.rowcps.basinpie.ui.BasinPieModel;
import usace.rowcps.computation.services.CalcFlowGroupTimeSeriesService;
import usace.rowcps.data.charttemplate.IChartTemplate;
import usace.rowcps.data.maptemplate.graphicoptions.ReleasesGraphicOptionData;
import usace.rowcps.data.maptemplate.reservoir.ReservoirGraphicOptionData;
import usace.rowcps.decisionsupport.ui.graphics.releases.ReleasesGraphicData;
import usace.rowcps.decisionsupport.ui.graphics.utilities.GraphicConstants;
import usace.rowcps.headless.calculator.AbstractScriptableCalc;
import usace.rowcps.regi.interfaces.model.ProjectChildLocationCacheService;
import usace.rowcps.regi.model.AtChartTemplateManager;
import usace.rowcps.regi.model.OptionalParams;
import usace.rowcps.regi.ui.gfx2d.PiePanel;
import usace.rowcps.decisionsupport.ui.basintree.OperationSupportBasinTreeModel;
import usace.rowcps.mappanel.ui.template.MapTemplateLayer;
import usace.rowcps.decisionsupport.ui.basintree.BasinTreeModel;
import usace.rowcps.decisionsupport.ui.graphics.releases.ReleasesGraphicPanel;
import usace.rowcps.decisionsupport.ui.reservoirplot.ReservoirPlotPanel;
import usace.rowcps.decisionsupport.ui.reservoirplot.ReservoirPlotPanelData;
import usace.rowcps.mappanel.ui.MapPanelDateRangeService;
import usace.rowcps.mappanel.ui.SimpleMapPanelDateRange;
import usace.rowcps.regi.basin.IBasinConnectivityModel;
import usace.rowcps.regi.executor.FutureDescriptor;
import usace.rowcps.regi.status.AtProjectManager;

/**
 *
 * @author ryan
 */
public class ScriptableStatusGraphicImpl extends AbstractScriptableCalc
        implements ScriptableCalc
{
    private static final Logger LOGGER = Logger.getLogger(ScriptableStatusGraphicImpl.class.getName());
    public final static String LATCH_SECONDS = "rowcps.latchseconds";

    public ScriptableStatusGraphicImpl(RegiDomain regiDomain, ManagerId manId)
    {
        super(regiDomain, manId);
        System.setProperty("java.awt.headless", "true");
    }

    public void generateReservoirStatusImage(String officeId, String locationId,
                                             String templateName, Date current,
                                             final int width, final int height,
                                             String filename) throws DbConnectionException, DbIoException, IOException, InterruptedException, ExecutionException, TimeoutException
    {
        SimpleMapPanelDateRange simpleDateRange = new SimpleMapPanelDateRange(current);
        MapPanelDateRangeService.registerRange(getManagerId(), simpleDateRange);
        
        LocationTemplate locTemp = new LocationTemplate(officeId, locationId);

        TimeInfo utcTimeInfo = getUtcTimeInfo(current, regiDomain.getTimeZone());

        TimeInfoSource utcTimeInfoSource = () -> utcTimeInfo;

        checkForKnownNeededServices();

        MapTemplateLayer mtl = getMapTemplateLayer(templateName);

        ReservoirGraphicOptionData optionData = mtl.getGraphicsOptions();

        CountDownLatch cdl = new CountDownLatch(1);
        PropertyChangeListener pcl = (PropertyChangeEvent evt) ->
        {
            if (GraphicConstants.DATA_FILLED_EVENT.equals(evt.getPropertyName()))
            {
                cdl.countDown();
            }
        };

        optionData.addPropertyChangeListener(pcl);

        AtProjectManager atProjectManager = this.regiDomain.getAtProjectManager(managerId);
        IProject iProject = atProjectManager.getIProject(locTemp, CacheUsage.NORMAL);

        ReservoirPlotPanelData plotPanelData = new ReservoirPlotPanelData(iProject, utcTimeInfoSource, managerId, optionData);
        plotPanelData.addPropertyChangeListener(pcl);

        ReservoirPlotPanel reservoirPlotPanel;

        RunnableFuture<ReservoirPlotPanel> panelFuture = new FutureTask<>(() -> new ReservoirPlotPanel(iProject, managerId, utcTimeInfoSource, plotPanelData));
        SwingUtilities.invokeLater(panelFuture);
        reservoirPlotPanel = panelFuture.get(1, TimeUnit.MINUTES);

        Integer seconds = Integer.getInteger(LATCH_SECONDS, 11 * 60); // This one goes to 11...
        boolean normalExit = cdl.await(seconds, TimeUnit.SECONDS);
        if (!normalExit)
        {
            LOGGER.log(Level.WARNING, "Timeout exceeded loading reservoir status graphic data.");
        }

        optionData.removePropertyChangeListener(pcl);

        // This next sleep is important b/c the latch gets set after the SwingWorker doInBackground complete but before done()
        // are called.   We need the done() methods to execute before we proceed.
        Thread.sleep(2000);

        String imageFormat = getFormatFromFile(filename);
		
		Dimension d = computePreferredSize(reservoirPlotPanel, width, height);
		
        layoutAndSave(reservoirPlotPanel, d, filename, imageFormat);
    }
	
	private Dimension computePreferredSize(Component comp, int requestedWidth, int requestedHeight)
	{
		comp.setPreferredSize(null);	//Causes the component to recompute its pref size
		Dimension prefSize = comp.getPreferredSize();
		Dimension reqSize = new Dimension(requestedWidth, requestedHeight);
		
		//This should prevent most clipping issues where the size provided is too small for the data.
		return computeLargestDimension(prefSize, reqSize);
	}
	
	private Dimension computeLargestDimension(Dimension dim1, Dimension dim2)
	{
		return new Dimension(Math.max(dim1.width, dim2.width), Math.max(dim1.height, dim2.height));
	}

    private boolean checkForKnownNeededServices()
    {
        boolean hasCalcFlow = hasGlobalService(CalcFlowGroupTimeSeriesService.class);
        if (!hasCalcFlow)
        {
            ServiceLoader serviceLoader = ServiceLoader.load(CalcFlowGroupTimeSeriesService.class);
            hasCalcFlow = hasService(CalcFlowGroupTimeSeriesService.class, serviceLoader);
        }

        if (!hasCalcFlow)
        {
            String mesg = "The CalcFlowGroupTimeSeriesService was not found and is known to be needed by Regi Headless.  "
                          + "Without this service the headless Status Graphic generation may not generate the correct values.  "
                          + "Even if the necessary classes are in the classpath, the services may still not be found "
                          + "if the jars do not include the necessary META-INF services folder.  "
                          + "For example, public-package-jars\\usace-rowcps-computation.jar contains implementation classes "
                          + "but not the service definitions.";
            LOGGER.warning(mesg);
        }

        boolean hasProjectChild = hasGlobalService(ProjectChildLocationCacheService.class);
        if (!hasProjectChild)
        {
            String mesg = "A ProjectChildLocationCacheService was not found and is known to be needed by Regi Headless.  "
                          + "Without this service the headless Status Graphic generation may not generate the correct values.  "
                          + "Even if the necessary classes are in the classpath, the services may still not be found "
                          + "if the jars do not include the necessary META-INF services folder.  "
                          + "For example, public-package-jars\\usace-rowcps-regi.jar contains implementation classes "
                          + "but not the service definitions.";
            LOGGER.warning(mesg);
        }

        return hasCalcFlow && hasProjectChild;
    }

    private boolean hasGlobalService(Class klass)
    {
        GlobalServiceLoaderDelegate instance = GlobalServiceLoader.getInstance();
        ServiceLoader serviceLoader = instance.getServiceLoader(klass);

        return hasService(klass, serviceLoader);
    }

    private boolean hasService(Class klass, ServiceLoader sl)
    {
        boolean hasCalcFlow = false;
        ServiceLoader serviceLoader;

        GlobalServiceLoaderDelegate instance = GlobalServiceLoader.getInstance();
        serviceLoader = instance.getServiceLoader(klass);

        for (Object service : serviceLoader)
        {
            hasCalcFlow = true;
            break;
        }
        return hasCalcFlow;
    }

    public void generateStreamStatusImage(String officeId, String locationId,
                                          String templateName, Date current,
                                          final int width, final int height,
                                          String filename) throws DbConnectionException, DbIoException, IOException, InterruptedException, ExecutionException, TimeoutException
    {

        LocationTemplate locTemp = new LocationTemplate(officeId, locationId);

        AtLocationManager locMan = this.regiDomain.getAtLocationManager(getManagerId());
        Location loc = locMan.retrieveLocation(locTemp, CacheUsage.NORMAL);

        final TimeInfo ti = getUtcTimeInfo(current, this.regiDomain.getTimeZone());

        TimeInfoSource timeInfoSource = () -> ti;

        SimpleMapPanelDateRange simpleDateRange = new SimpleMapPanelDateRange(current);
        MapPanelDateRangeService.registerRange(getManagerId(), simpleDateRange);
        
        MapTemplateLayer mapTemplateLayer = getMapTemplateLayer(templateName);        
        
        if(mapTemplateLayer == null)
        {
            LOGGER.log(Level.SEVERE, "Unable to locate MapTemplateLayer with the name:{0}", templateName);
            return;
        }                       
        
        StreamGageGraphicOptionData sggod = mapTemplateLayer.getStreamGageGraphicOptions();
        TimeZone timeZone = this.regiDomain.getTimeZone();
        final StreamData streamData = new StreamData(loc, sggod, timeInfoSource, timeZone, getManagerId());

        final CountDownLatch cdl = new CountDownLatch(1);
        PropertyChangeListener pcl = (PropertyChangeEvent evt) ->
        {
            if (GraphicConstants.DATA_FILLED_EVENT.equals(evt.getPropertyName()))
            {
                cdl.countDown();
            }
        };

        streamData.addPropertyChangeListener(pcl);
		Future future = streamData.retrieveData();
        future.get(11, TimeUnit.MINUTES);	//No return from this, but we need to wait until it's done.

        String imageFormat = getFormatFromFile(filename);
		
		StreamPlotPanel panel = new StreamPlotPanel(streamData);  // Why is this building a panel on background thread?
		
		Dimension d = computePreferredSize(panel, width, height);

        layoutAndSave(panel, d, filename, imageFormat);
    }
    
    public void generateReleasesStatusImage(String officeId, String locationId,
                                            String templateName, Date current,
                                            final int width, final int height,
                                            String filename) throws DbConnectionException, DbIoException, IOException, InterruptedException, TimeoutException, ExecutionException
    {
        Metrics metrics = MetricsServiceProvider.createMetrics(getClass().getSimpleName());

        MyReleasesGraphicData data = buildReleasesGraphicData(officeId, locationId, current, metrics);

		CompletableFuture<Void> future = data.retrieveOutletGroups(new OptionalParams(metrics));
        data.updateDataScopeSynchronous(new OptionalParams(metrics));
		LOGGER.log(Level.FINE, "(Headless)Data scope retrieval complete.");
		future.get();
		LOGGER.log(Level.FINE, "(Headless)Retrieval of outlet groups complete.");
        
		//This needs to occur *after* all of the futures have been completed.
		data.fireDataUpdateEvent();
		
		saveReleasesToFile(data, width, height, filename);
    }

	private MyReleasesGraphicData buildReleasesGraphicData(String officeId, String locationId, Date current, Metrics metrics) throws DbConnectionException, DbIoException
	{
		LocationTemplate locTemp = new LocationTemplate(officeId, locationId);
		AtLocationManager locMan = this.regiDomain.getAtLocationManager(getManagerId());
		Location loc = locMan.retrieveLocation(locTemp, CacheUsage.NORMAL);
		TimeInfo utcTimeInfo = getUtcTimeInfo(current, this.regiDomain.getTimeZone());
		TimeInfoSource utcTimeInfoSource = () -> utcTimeInfo;
		SimpleMapPanelDateRange simpleDateRange = new SimpleMapPanelDateRange(current);
		MapPanelDateRangeService.registerRange(getManagerId(), simpleDateRange);
		ReleasesGraphicOptionData graphicOptionData = new ReleasesGraphicOptionData();
		
		return new MyReleasesGraphicData(loc, managerId, utcTimeInfoSource, graphicOptionData, new OptionalParams(metrics));
	}

	private void saveReleasesToFile(ReleasesGraphicData data, int width, int height, String filename)
	{
		try
		{
			SwingUtilities.invokeAndWait(() ->
			{
				ReleasesGraphicPanel releasesGraphicPanel = new ReleasesGraphicPanel(false, true);
				releasesGraphicPanel.setData(data);
				
				//This is a bit assinine, but it's used to massage the preferred sizes
				//The detail component's pref size is constantly recomputed on paint....why?  because science!  and my youthful ignorance.
				renderGraphic(releasesGraphicPanel);
				renderGraphic(releasesGraphicPanel);
				renderGraphic(releasesGraphicPanel);
				renderGraphic(releasesGraphicPanel);
				
				Dimension imageDimension = computePreferredSize(releasesGraphicPanel, width, height);
				String imageFormat = getFormatFromFile(filename);
				
				try
				{
					layoutAndSave(releasesGraphicPanel, imageDimension, filename, imageFormat);
				}
				catch (IOException ex)
				{
					LOGGER.log(Level.SEVERE, "Unable to save to file " + filename, ex);
				}
			});
		}
		catch (InterruptedException ex)
		{
			LOGGER.log(Level.SEVERE, "Event thread was interrupted", ex);
		}
		catch (InvocationTargetException ex)
		{
			LOGGER.log(Level.SEVERE, "An uncaught exception occurred on the Event Thread for generateReleasesStatusImage.", ex);
		}
	}

	private void renderGraphic(JComponent comp)
	{
		layoutComponent(comp, comp.getSize());
		BufferedImage img = new BufferedImage(comp.getWidth(), comp.getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		comp.print(g);
		computePreferredSize(comp, comp.getWidth(), comp.getHeight());
	}

    private void layoutComponent(JComponent component, Dimension d)
    {
        LayoutManager layout = component.getLayout();
        if (layout != null)
        {
            layout.layoutContainer(component);
        }
        component.setSize(d);
        component.addNotify();
        component.invalidate();
        component.validate();
        component.addNotify();
        if (layout != null)
        {
            layout.layoutContainer(component);
        }
    }

    public static class MyReleasesGraphicData extends ReleasesGraphicData
    {
        private boolean _hasRetrieved = false;

        public MyReleasesGraphicData(Location loc, ManagerId manId,
                                     TimeInfoSource tis,
                                     ReleasesGraphicOptionData options,
                                     OptionalParams optionalParams)
        {
            super(loc, manId, tis, options, optionalParams);
        }

		@Override
		public CompletableFuture<Void> updateDataScope(OptionalParams params)
		{
			if (!_hasRetrieved)
			{
				LOGGER.log(Level.FINE, "(Headless)Updating data scope");
				_hasRetrieved = true;
				return super.updateDataScope(params);
			}
			return CompletableFuture.completedFuture(null);
		}
		
		public void updateDataScopeSynchronous(OptionalParams params) throws InterruptedException, ExecutionException
		{
			updateDataScope(params).get();
		}

		@Override
		public void fireDataUpdateEvent()
		{
			//Exposing this publicly
			super.fireDataUpdateEvent(); //To change body of generated methods, choose Tools | Templates.
		}
    }

    public TimeInfo getUtcTimeInfo(Date current, TimeZone displayTimeZone)
    {
        // The time controls internally use HecTimes with utc values and Regi formats them to the display timezone in the ui
        // components.
        // In headless the user is giving us a Date object and a timezone that date object is in.
        // So imagine the user wants a graphic displayed at midnight on 4/18 in CDT.  Say that UTC is ahead of CDT by 5 hours
        // so we need hectime to actually store the time at 5AM
        HecTime utcStart = new HecTime();        
        utcStart.setTimeInMillis(current.getTime());

        Calendar endCal = Calendar.getInstance(displayTimeZone);
        endCal.setTime(current);
        endCal.add(Calendar.DAY_OF_MONTH, 1);
        //truncate down to start of day //
        HecTime utcEnd = new HecTime();
        utcEnd.setTimeInMillis(endCal.getTimeInMillis());

        HecTime utcCurrent = new HecTime();
        utcCurrent.setTimeInMillis(current.getTime());
        
        utcCurrent.showTimeAsBeginningOfDay(true);

        int stepSize = 1000 * 60 * 60;  // millisPerHour
        TimeInfo ti = new TimeInfo(utcStart, utcEnd, utcCurrent, stepSize);
        return ti;
    }

    private List<IMapTemplate> getMapTemplates() throws DbIoException, DbConnectionException
    {
        RegiDomain currentProject = (RegiDomain) RegiDomain.getCurrentProject();
        final AtMapTemplateManager tm = currentProject.getAtMapTemplateManager(getManagerId());
        final List<IMapTemplate> mapTemplates = tm.retrieveMapTemplates(currentProject.getUserOfficeId(),
                                                                        CacheUsage.NORMAL);
        return mapTemplates;
    }

    private MapTemplateLayer getMapTemplateLayer(String templateName) throws DbIoException, DbConnectionException
    {
        MapTemplateLayer retval = null;

        List<IMapTemplate> mapTemplates = getMapTemplates();

        IMapTemplate matching = find(templateName, mapTemplates);

        if (matching != null)
        {
            retval = new MapTemplateLayer(matching, getManagerIdProvider());
        }

        return retval;
    }

    private IMapTemplate find(String templateName,
                              List<IMapTemplate> mapTemplates)
    {
        IMapTemplate retval = null;

        if (mapTemplates != null && !mapTemplates.isEmpty() && templateName != null)
        {
            for (IMapTemplate mapTemplate : mapTemplates)
            {
                if (templateName.equalsIgnoreCase(mapTemplate.getName()))
                {
                    retval = mapTemplate;
                    break;
                }
            }
        }

        return retval;
    }

    public void generateBasinPieImageForGroup(final String officeId,
                                      final String locationStr,
                                      final String groupId,
                                      final Date date, final int width,
                                      final int height, final String template,
                                      final String file) throws Exception
    {
        String[] locations = new String[] {locationStr};
        Date[] dates = new Date[] {date};
        String[] templates = new String[]{template};
        generateBasinPieImagesForGroup(officeId, locations, groupId, dates, width, 
                                    height, templates, file);
    }
    
    public void generateBasinPieImageForBasin(final String officeId,
                                      final String locationStr,
                                      final String basinId,
                                      final Date date, final int width,
                                      final int height, final String template,
                                      final String file) throws Exception
    {
        String[] locations = new String[] {locationStr};
        Date[] dates = new Date[] {date};
        String[] templates = new String[]{template};
        generateBasinPieImagesForBasin(officeId, locations, basinId, dates, width, 
                                    height, templates, file);
    }

/**
     * This method generates a suite a basin pie images for a given basin
     * 1 basin pie image for each possible assigned location as a reference.
     * 
     * @param officeId
     * @param basinId
     * @param dates
     * @param width
     * @param height
     * @param templateIds
     * @param file
     * @throws Exception 
     */
    public void generateAllBasinPieImagesForBasin(final String officeId,
                                               final String basinId,
                                               final Date[] dates,
                                               final int width, final int height,
                                               final String[] templateIds,
                                               final String file) throws Exception
    {
        LocationGroupFactory locationGroupFactory = new LocationGroupFactory(getManagerIdProvider());        
        LocationGroup locationGroup = locationGroupFactory.retrieveLocationGroupForBasin(basinId);
        
        OperationSupportBasinTreeModel basinTreeModel = new OperationSupportBasinTreeModel(null);
        IBasinConnectivityModel basinConnModel = locationGroupFactory.getBasinConnectivityModel();
        basinTreeModel.fillBasinTree(locationGroup, basinConnModel);        
        
        generateAllBasinPieImages(officeId, locationGroup, basinTreeModel, dates, width, height, templateIds, file);
    }    
    
    /**
     * This method generates a suite a basin pie images for a given basin
     * 1 basin pie image for each possible assigned location as a reference.
     * 
     * @param officeId
	 * @param groupId
     * @param dates
     * @param width
     * @param height
     * @param templateIds
     * @param file
     * @throws Exception 
     */
    public void generateAllBasinPieImagesForGroup(final String officeId,
                                               final String groupId,
                                               final Date[] dates,
                                               final int width, final int height,
                                               final String[] templateIds,
                                               final String file) throws Exception
    {
        LocationGroupFactory locationGroupFactory = new LocationGroupFactory(getManagerIdProvider());        
        LocationGroup locationGroup = locationGroupFactory.retrieveProjectGroup(groupId);
        
        BasinTreeModel basinTreeModel = new BasinTreeModel(null);
        basinTreeModel.fillBasinTree(locationGroup);
        
        generateAllBasinPieImages(officeId, locationGroup, basinTreeModel, dates, width, height, templateIds, file);
    }
    
    private void generateAllBasinPieImages(final String officeId,
                                               final LocationGroup locationGroup,
                                               final BasinTreeModel basinTreeModel,
                                               final Date[] dates,
                                               final int width, final int height,
                                               final String[] templateIds,
                                               final String file) throws Exception
    {
        List<LocationTemplate> referenceLocations = new ArrayList<>();
        
        Set<AssignedLocation> assignedLocations = locationGroup.getAssignedLocations();
        for (AssignedLocation assignedLocation : assignedLocations)
        {
            referenceLocations.add(assignedLocation.getLocRef());
        }

        List<IChartTemplate> templates = getTemplates(templateIds, officeId);
        final Dimension d = new Dimension(width, height);
        String imageFormat = getFormatFromFile(file);

        generateImages(officeId, referenceLocations, locationGroup, basinTreeModel, dates, d, templates, file, imageFormat);
    }

    public void generateBasinPieImagesForBasin(final String officeId,
                                       final String locationStrs[],
                                       final String basinId,
                                       final Date[] dates, final int width,
                                       final int height,
                                       final String[] templateIds,
                                       final String filename) throws Exception
    {
        System.setProperty("java.awt.headless", "true");
        if (width <= 0 || height <= 0)
        {  // is there a max?
            LOGGER.warning("Width and Height parameters must be > 0");
            return;
        }
        
        LocationGroupFactory locationGroupFactory = new LocationGroupFactory(getManagerIdProvider());
        LocationGroup locationGroup = locationGroupFactory.retrieveLocationGroupForBasin(basinId);
        
        OperationSupportBasinTreeModel basinTreeModel = new OperationSupportBasinTreeModel(null);
        IBasinConnectivityModel basinConnModel = locationGroupFactory.getBasinConnectivityModel();
        basinTreeModel.fillBasinTree(locationGroup, basinConnModel);
        
        List<IChartTemplate> templates = getTemplates(templateIds, officeId);

        List<LocationTemplate> locs = new ArrayList<>();
        for (String locationStr : locationStrs)
        {
            final LocationTemplate locRef = new LocationTemplate(officeId, locationStr);
            locs.add(locRef);
        }

        String imageFormat = getFormatFromFile(filename);
        final Dimension dimension = new Dimension(width, height);

        generateImages(officeId, locs, locationGroup, basinTreeModel, dates, dimension, templates, filename, imageFormat);
    }
    
    public void generateBasinPieImagesForGroup(final String officeId,
                                       final String locationStrs[],
                                       final String groupId,
                                       final Date[] dates, final int width,
                                       final int height,
                                       final String[] templateIds,
                                       final String filename) throws Exception
    {
        System.setProperty("java.awt.headless", "true");
        if (width <= 0 || height <= 0)
        {  // is there a max?
            LOGGER.warning("Width and Height parameters must be > 0");
            return;
        }
        
        LocationGroupFactory locationGroupFactory = new LocationGroupFactory(getManagerIdProvider());               
        LocationGroup locationGroup = locationGroupFactory.retrieveProjectGroup(groupId);
        BasinTreeModel treeModel = new BasinTreeModel();
        treeModel.fillBasinTree(locationGroup);        
        
        List<IChartTemplate> templates = getTemplates(templateIds, officeId);

        List<LocationTemplate> locs = new ArrayList<>();
        for (String locationStr : locationStrs)
        {
            final LocationTemplate locRef = new LocationTemplate(officeId, locationStr);
            locs.add(locRef);
        }

        String imageFormat = getFormatFromFile(filename);
        final Dimension dimension = new Dimension(width, height);

        generateImages(officeId, locs, locationGroup, treeModel, dates, dimension, templates, filename, imageFormat);
    }

    public void generateImages(String officeId,
                               List<LocationTemplate> referenceLocations, 
							   LocationGroup locationGroup,
                               BasinTreeModel treeModel,
                               Date[] dates,
							   Dimension d,
                               List<IChartTemplate> templates,
                               String filePattern, String imageFormat)
			throws ExecutionException, DbConnectionException, InterruptedException
    {
        SortedSet<Date> dateSet = new TreeSet<>();
        dateSet.addAll(Arrays.asList(dates));

        Date startDate = dateSet.first();
        Date endDate = dateSet.last();
        
        for (IChartTemplate chartTemplate : templates)
        {
            LOGGER.log(Level.INFO, "Generating images for template:{0}", chartTemplate.getId());
            BasinPieModel pieModel = buildAndInitializeBasinPieModel(locationGroup, chartTemplate, startDate, endDate, treeModel);

            for (Date date : dateSet)
            {
                for (LocationTemplate locRef : referenceLocations)
                {
                    String file = getFileName(date, filePattern, locRef.getLocationId(), imageFormat, chartTemplate.getIdSuffix(), officeId, locationGroup.getName(), d.width, d.height);
                    drawImage(treeModel, locRef, pieModel, d, date, file, imageFormat);
                }
            }
        }
    }

    public List<IChartTemplate> getTemplates(final String[] templateIds,
                                             final String officeId) throws DbConnectionException, DbIoException
    {
        List<IChartTemplate> templates = new ArrayList<>();
        AtChartTemplateManager chartTemplateManager = regiDomain.getAtChartTemplateManager(managerId);
        for (String template : templateIds)
        {
            String templateId = IChartTemplate.CHART_TEMPLATE_CATEGORY_ID + "." + officeId + "." + template;

            IChartTemplate chartTemplate = chartTemplateManager.retrieveChartTemplate(officeId, templateId, CacheUsage.NORMAL);
            if (chartTemplate == null)
            {
                // try again using the user-provided string.
                chartTemplate = chartTemplateManager.retrieveChartTemplate(officeId, template, CacheUsage.NORMAL);
            }

            if (chartTemplate == null)
            {
                LOGGER.log(Level.WARNING, "Could not locate chart:{0}", templateId);
            }
            else
            {
                templates.add(chartTemplate);
            }

        }
        return templates;
    }

    public static String getFileName(Date date, final String filePattern,
                                     final String locationStr,
                                     String imageFormat, String chartTemplate,
                                     String officeId, String basinId, int width,
                                     int height)
    {

        String dateStr = getDateString(date);

        SimpleTemplateEngine engine = new SimpleTemplateEngine();
        engine.addPattern("date", dateStr);
        engine.addPattern("office_id", officeId);
        engine.addPattern("location_id", locationStr);
        engine.addPattern("chart_template_id", chartTemplate);
        engine.addPattern("basin_id", basinId);
        engine.addPattern("image_format", imageFormat);
        engine.addPattern("width", Integer.toString(width));
        engine.addPattern("height", Integer.toString(height));

        String filename = engine.makeReplacements(filePattern);
//		Replace anything that isn't a-z or A-z or 0-9 or [:\/)(.-] with an underscore.
        return filename.replaceAll("[^a-zA-Z0-9:\\\\\\/\\)\\(\\.\\- ]", "_");
    }

    public static String getDateString(Date date)
    {
        Instant asInstant = date.toInstant();
        String dateStr = asInstant.toString();  // like: 2017-02-24T22:23:21.149Z
        dateStr = dateStr.replaceAll(":", "_");
        return dateStr;
    }

    public void drawImage(BasinTreeModel treeModel,
                          LocationTemplate locRef,
                          BasinPieModel pieModel, Dimension d,
                          Date date, String file,
                          String imageFormat) throws InterruptedException, ExecutionException
    {

        final CountDownLatch latch = new CountDownLatch(1);
        final SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>()
        {
            @Override
            protected Void doInBackground() throws Exception
            {
                List<LocationTemplate> relavantLocations = treeModel.getRelevantLocations(locRef);
                LOGGER.log(Level.INFO, "Found {0} locations relevant to {1}", new Object[]
                   {
                       relavantLocations.size(), locRef
                });
                pieModel.setActiveLocations(relavantLocations, true);
                
                return null;
            }

            @Override
            protected void done()
            {
                try
                {
                    LOGGER.log(Level.INFO, "Creating Basin Pie Panel.");
                    writeBasinImage(d, pieModel, date, file, imageFormat);
                    latch.countDown();
                }
                catch (IOException ex)
                {
                    LOGGER.log(Level.SEVERE, null, ex);
                }
            }
        };

        worker.execute();

        Void got = worker.get();  // This returns as soon as doInBackground finishes.
        boolean normalExit = latch.await(Integer.getInteger(LATCH_SECONDS, 11 * 60), TimeUnit.SECONDS);
        if (!normalExit)
        {
            LOGGER.log(Level.WARNING, "Exceeded timeout waiting for basin image to draw.");
        }

    }

    public BasinPieModel buildAndInitializeBasinPieModel(
            LocationGroup locationGroup, IChartTemplate chartTemplate,
            Date startDate, Date endDate,
            BasinTreeModel treeModel) throws InterruptedException
    {
        BasinPieModel pieModel = new BasinPieModel(managerId, locationGroup, chartTemplate, startDate, endDate);
        CountDownLatch initlatch = new CountDownLatch(1);
        pieModel.addPropertyChangeListener((PropertyChangeEvent evt) ->
        {
            if ("PieDataChangedProperty".equals(evt.getPropertyName()))
            {
                LOGGER.log(Level.FINER, "PieDataChanged edt:{0} latch:{1}",
                                            new Object[]
                                            {
                                                SwingUtilities.isEventDispatchThread(), initlatch.getCount()
                                            });
                initlatch.countDown();
            }
        });
        List<LocationTemplate> forInitCache = treeModel.getRelevantLocations(null);
        LOGGER.log(Level.INFO, "Initializing BasinPieModel with {0} locations.", forInitCache.size());
        pieModel.initCache(forInitCache);  // this can take a while but it fires a property change event when its done.

        boolean normalExit = initlatch.await(Integer.getInteger(LATCH_SECONDS, 11 * 60), TimeUnit.SECONDS);
        if (!normalExit)
        {
            LOGGER.log(Level.WARNING, "Exceeded timeout waiting for basin pie model to load.");
        }

        return pieModel;
    }

    private void writeBasinImage(Dimension d, BasinPieModel pieModel,
                                 Date date, String file, String imageFormat) throws FileNotFoundException, IOException
    {
        LocationGroup locationGroup = pieModel.getLocationGroup();
        PiePanel piePanel = new PiePanel();
        IChartTemplate chartTemplate = pieModel.getChartTemplate();
        List<String> dataIdentifiers = chartTemplate.getDataIdentifiers();
        Set<String> poolIds = new HashSet<>();
        poolIds.addAll(dataIdentifiers);
        
        TimeZone timezone = regiDomain.getTimeZone();
        piePanel.setTimeZone(timezone);

        JLayer piePanelJLayerWrapper = new JLayer(piePanel);
        HeadlessBasinPieAnnotationLayer basinPieAnnotationLayer = new HeadlessBasinPieAnnotationLayer(getManagerIdProvider(), locationGroup, pieModel.getActiveLocations());
        basinPieAnnotationLayer.fillPanel(poolIds, date, chartTemplate);
        
        PinnableComponentGlassPane glassPane = PinnableComponentGlassPaneFactory.createNewGlassPane(basinPieAnnotationLayer, piePanel);

        piePanelJLayerWrapper.setGlassPane(glassPane);
        glassPane.addPinnableContainer(basinPieAnnotationLayer);
        PinnableContainer container = glassPane.getPinnableContainer(basinPieAnnotationLayer);
        basinPieAnnotationLayer.setPinnableContainer(container);
        container.setSize(d);

        List<CompletableFuture<Void>> futures = basinPieAnnotationLayer.resetFromChartTemplate();
        futures.stream().map((future) -> 
        {
            if (future instanceof FutureDescriptor)
            {
                return ((FutureDescriptor<Void>) future).getFuture();
            }
            return future;
        }).forEach((future)->
        {
            try
            {
                future.get();
            }
            catch (InterruptedException | ExecutionException ex)
            {
                LOGGER.log(Level.INFO, "Well that didn't work...", ex);
            }
        });
        
        PinnableComponentGlassPaneFactory.getGlassPane(basinPieAnnotationLayer).setVisible(true);

        LOGGER.fine("Filling panel with model.");
        piePanel.fillPanel(pieModel);
        LOGGER.log(Level.INFO, "Setting active date:{0}", date);
        piePanel.setActiveDate(date);

		//This updates the data of bar charts.  This has to happen after we've loaded the data.  Otherwise it doesn't
		//Load the bar chart correctly.
		basinPieAnnotationLayer.updateBarChartGraphics();

        layoutAndSave(piePanelJLayerWrapper, d, file, imageFormat);
    }

    /**
     * Saves a copy of the plot as a image type
     *
     * @param os the output stream to write to.
     * @param panel
     * @param imageType i.e. "png" or "jpg".
     * @param compression sets the compression for the image if the image writer
     * supports it
     * @return 
     * @throws java.io.IOException 
     */
    protected boolean saveToStream(OutputStream os, final JComponent panel,
                                   String imageType, float compression) throws IOException
    {
        // This next comment came from the source this was loosely based on.
        /**
         * Add a revalidate here in order for headless scripting to work. Due to
         * a joining of the building and displaying of the plot in showPlot(),
         * any component changes to recompute preferred sizes and exports look
         * bad and components overlap. If we do this here, then we can build and
         * show the plot, tweak some properties and export correctly.
         */

        BufferedImage bImage;

        if (SwingUtilities.isEventDispatchThread())
        {
            bImage = saveToImage(panel.getSize(), panel);
        }
        else
        {
            Callable<BufferedImage> imageCallable = () -> saveToImage(panel.getSize(), panel);
            FutureTask<BufferedImage> futureTask = new FutureTask<>(imageCallable);

            SwingUtilities.invokeLater(futureTask);
            try
            {
                // Not sure how long to wait here.  Infinite is wrong.
                bImage = futureTask.get(5, TimeUnit.MINUTES);
            }
            catch (InterruptedException ex)
            {
                Thread.currentThread().interrupt();
                throw new IOException("Background paint was interrupted.", ex);
            }
            catch (ExecutionException ex)
            {
                throw new IOException("Background paint encountered ExecutionException.", ex);
            }
            catch (TimeoutException ex)
            {
                throw new IOException("Timeout waiting for paint to complete.", ex);
            }
        }
        return writeImage(imageType, compression, os, bImage);
    }

    public boolean writeImage(String imageType, float compression,
                              OutputStream os, BufferedImage bImage)
    {
        Iterator<ImageWriter> iter = javax.imageio.ImageIO.getImageWritersByFormatName(imageType);
        if (!iter.hasNext())
        {
            LOGGER.log(Level.WARNING, "No Image writers exist for Image Type = {0}", imageType);
            return true;
        }
        ImageWriter next = iter.next();
        ImageWriteParam defaultWriteParam = next.getDefaultWriteParam();
        if (defaultWriteParam.canWriteCompressed() && compression != hec.lang.Const.UNDEFINED_INT)
        {
            defaultWriteParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            defaultWriteParam.setCompressionQuality(compression / 100);
        }
        writeToStream(next, os, bImage, defaultWriteParam);
        return false;
    }

    public static BufferedImage saveToImage(Dimension d, JComponent panel)
    {

        BufferedImage bImage = new BufferedImage(d.width, d.height, BufferedImage.TYPE_INT_RGB);
        paintIntoImage(bImage, d, panel);

        return bImage;
    }

    private static void paintIntoImage(BufferedImage bImage, Dimension d,
                                       JComponent panel)
    {
        Graphics2D g = bImage.createGraphics();

        boolean useTrans = false;

        if (!useTrans)
        {
            Color color = new Color(226, 226, 226);  // make backgroup grey
            g.setColor(color);
            g.fillRect(0, 0, d.width, d.height);
        }
        else
        {
            // This doesn't work.  
            Color color = new Color(0, 0, 0, 0);
            g.setColor(color);
            g.setComposite(AlphaComposite.Clear);
            g.fillRect(0, 0, d.width, d.height);
            g.setComposite(AlphaComposite.SrcOver);
        }

        panel.paint(g);
        g.dispose();
    }

    public static void writeToStream(ImageWriter writer, OutputStream fs,
                                     BufferedImage bImage,
                                     ImageWriteParam defaultWriteParam)
    {
        try
        {
            writer.setOutput(javax.imageio.ImageIO.createImageOutputStream(fs));
            writer.write(null, new IIOImage(bImage, null, null), defaultWriteParam);
        }
        catch (IOException e)
        {
            LOGGER.log(Level.SEVERE, "Exception encountered writing image.", e);
        }
        finally
        {
            writer.dispose();
            try
            {
                if (fs != null)
                {
                    fs.flush();
                    fs.close();
                }
            }
            catch (IOException ioe)
            {
                LOGGER.log(Level.SEVERE, "IOException encountered while closing OutputStream.", ioe);
            }
        }
    }

    protected String getFormatFromFile(String file)
    {
        String format = "png";

        if (file != null && !file.isEmpty())
        {
            String asLower = file.toLowerCase();

            if (asLower.endsWith(".jpg") || asLower.endsWith(".jpeg"))
            {
                return "jpg";
            }

        }

        return format;
    }

    public void layoutAndSave(final JComponent component, final Dimension d,
                              String file, String imageFormat) throws IOException
    {
        if (SwingUtilities.isEventDispatchThread())
        {
            layoutComponent(component, d);
        }
        else
        {
            Callable<Boolean> imageCallable = () ->
            {
                layoutComponent(component, d);
                return Boolean.TRUE;
            };
            FutureTask<Boolean> futureTask = new FutureTask<>(imageCallable);

            SwingUtilities.invokeLater(futureTask);
            try
            {
                // Not sure how long to wait here.  Infinite is wrong.
                Boolean dontcare = futureTask.get(1, TimeUnit.MINUTES);
            }
            catch (InterruptedException ex)
            {
                Thread.currentThread().interrupt();
                throw new IOException("Background layout was interrupted.", ex);
            }
            catch (ExecutionException ex)
            {
                throw new IOException("Background layout encountered ExecutionException.", ex);
            }
            catch (TimeoutException ex)
            {
                throw new IOException("Timeout waiting for layout to complete.", ex);
            }
        }

        File f = new File(file);
        f.getParentFile().mkdirs();

        try (FileOutputStream fos = new FileOutputStream(f);
             BufferedOutputStream bos = new BufferedOutputStream(fos);)
        {
            LOGGER.info("Writing to output stream");
            saveToStream(bos, component, imageFormat, 100.0f);
        }
    }    
}
