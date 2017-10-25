package usace.rowcps.headless.calculator.status;

import com.rma.ui.pinnable.PinnableComponentGlassPane;
import com.rma.ui.pinnable.PinnableComponentGlassPaneFactory;
import com.rma.ui.pinnable.PinnableContainer;
import hec.data.location.AssignedLocation;
import hec.data.location.Location;
import hec.data.location.LocationCategoryRef;
import hec.data.location.LocationGroup;
import hec.data.location.LocationGroupRef;
import hec.data.location.LocationTemplate;
import hec.db.DbConnectionException;
import hec.db.DbIoException;
import hec.heclib.util.HecTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import usace.rowcps.data.maptemplate.IMapTemplate;
import usace.rowcps.data.maptemplate.streamplot.StreamGageGraphicOptionData;
import usace.rowcps.decisionsupport.ui.mappanel.template.MapTemplateLayer;
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
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.Instant;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.SortedSet;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
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
import usace.rowcps.computation.common.IEventThreadExceptionProcessor;
import usace.rowcps.computation.services.CalcFlowGroupTimeSeriesService;
import usace.rowcps.data.basin.IBasin;
import usace.rowcps.data.charttemplate.IChartTemplate;
import usace.rowcps.data.maptemplate.graphicoptions.ReleasesGraphicOptionData;
import usace.rowcps.data.maptemplate.reservoir.ReservoirGraphicOptionData;
import usace.rowcps.data.project.IProject;
import usace.rowcps.data.stream.IStreamLocation;
import usace.rowcps.decisionsupport.ui.DecisionSupportEditor;
import usace.rowcps.decisionsupport.ui.basinconnectivity.BasinConnectivityDataAdapter;
import usace.rowcps.decisionsupport.ui.basinpie.BasinPieModel;
import usace.rowcps.decisionsupport.ui.basinpie.annotations.BasinPieAnnotationLayer;
import usace.rowcps.decisionsupport.ui.graphics.releases.ReleasesGraphicData;
import usace.rowcps.decisionsupport.ui.graphics.utilities.GraphicConstants;
import usace.rowcps.decisionsupport.ui.mappanel.OperationSupportBasinTreeModel;
import usace.rowcps.decisionsupport.ui.reservoirplot.ReservoirPlotPanel;
import usace.rowcps.decisionsupport.ui.reservoirplot.ReservoirPlotPanelData;
import usace.rowcps.headless.calculator.AbstractScriptableCalc;
import usace.rowcps.regi.interfaces.model.ProjectChildLocationCacheService;

import usace.rowcps.regi.model.AtBasinManager;
import usace.rowcps.regi.model.AtChartTemplateManager;
import usace.rowcps.regi.model.AtProjectManager;
import usace.rowcps.regi.model.OptionalParams;
import usace.rowcps.regi.ui.gfx2d.PiePanel;

/**
 *
 * @author ryan
 */
public class ScriptableStatusGraphicImpl extends AbstractScriptableCalc implements ScriptableCalc {

	private static final Logger logger = Logger.getLogger(ScriptableStatusGraphicImpl.class.getName());

	public final static String LATCH_SECONDS = "rowcps.latchseconds";

	public ScriptableStatusGraphicImpl(RegiDomain regiDomain, ManagerId manId) {
		super(regiDomain, manId);
		System.setProperty("java.awt.headless", "true");
	}

	public void generateReservoirStatusImage(String officeId, String locationId, String templateName, Date current,
			final int width, final int height, String filename) throws DbConnectionException, DbIoException, IOException, InterruptedException, ExecutionException, TimeoutException {

		LocationTemplate locTemp = new LocationTemplate(officeId, locationId);

		AtLocationManager locMan = this.regiDomain.getAtLocationManager(getManagerId());
		Location loc = locMan.retrieveLocation(locTemp, CacheUsage.NORMAL);

		final TimeInfo ti = getTimeInfo(current, this.regiDomain.getTimeZone());

		final TimeInfoSource tis = new TimeInfoSource() {
			@Override
			public hec.map.geoui.interp.TimeInfo getTimeInfo() {
				return ti;
			}
		};

		checkForKnownNeededServices();

		AtMapTemplateManager atMapTemplateManager = this.regiDomain.getAtMapTemplateManager(managerId);

		MapTemplateLayer mtl = getMapTemplateLayer(templateName);

		final ReservoirGraphicOptionData rgod = mtl.getGraphicsOptions();

		final CountDownLatch cdl = new CountDownLatch(1);
		PropertyChangeListener pcl = new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				if (GraphicConstants.DATA_FILLED_EVENT.equals(evt.getPropertyName())) {
					cdl.countDown();
				}
			}
		};

		rgod.addPropertyChangeListener(pcl);

		AtProjectManager atProjectManager = this.regiDomain.getAtProjectManager(managerId);
		final IProject iProject = atProjectManager.getIProject(locTemp, CacheUsage.NORMAL);

		final ReservoirPlotPanelData rppd = new ReservoirPlotPanelData(iProject, tis, managerId, rgod);
		rppd.addListener(pcl);

		ReservoirPlotPanel rpp;

		RunnableFuture<ReservoirPlotPanel> panelFuture = new FutureTask<ReservoirPlotPanel>(new Callable<ReservoirPlotPanel>() {
			@Override
			public ReservoirPlotPanel call() throws Exception {
				return new ReservoirPlotPanel(iProject, managerId, tis, rgod, rppd);
			}
		});
		SwingUtilities.invokeLater(panelFuture);
		rpp = panelFuture.get(1, TimeUnit.MINUTES);

		Integer seconds = Integer.getInteger(LATCH_SECONDS, 11 * 60); // This one goes to 11...
		boolean normalExit = cdl.await(seconds, TimeUnit.SECONDS);
		if (!normalExit) {
			logger.log(Level.WARNING, "Timeout exceeded loading reservoir status graphic data.");
		}

		rgod.removePropertyChangeListener(pcl);

		// This next sleep is important b/c the latch gets set after the SwingWorker doInBackground complete but before done()
		// are called.   We need the done() methods to execute before we proceed.
		Thread.sleep(2000);

		String imageFormat = getFormatFromFile(filename);
		final Dimension d = new Dimension(width, height);
		layoutAndSave(rpp, d, filename, imageFormat);
	}

	private boolean checkForKnownNeededServices() {
		boolean retval = false;

		boolean hasCalcFlow = hasGlobalService(CalcFlowGroupTimeSeriesService.class);
		if (!hasCalcFlow) {
			ServiceLoader serviceLoader = ServiceLoader.load(CalcFlowGroupTimeSeriesService.class);
			hasCalcFlow = hasService(CalcFlowGroupTimeSeriesService.class, serviceLoader);
		}

		if (!hasCalcFlow) {
			String mesg = "The CalcFlowGroupTimeSeriesService was not found and is known to be needed by Regi Headless.  "
					+ "Without this service the headless Status Graphic generation may not generate the correct values.  "
					+ "Even if the necessary classes are in the classpath, the services may still not be found "
					+ "if the jars do not include the necessary META-INF services folder.  "
					+ "For example, public-package-jars\\usace-rowcps-computation.jar contains implementation classes "
					+ "but not the service definitions.";
			logger.warning(mesg);
		}

		boolean hasProjectChild = hasGlobalService(ProjectChildLocationCacheService.class);
		if (!hasCalcFlow) {
			String mesg = "A ProjectChildLocationCacheService was not found and is known to be needed by Regi Headless.  "
					+ "Without this service the headless Status Graphic generation may not generate the correct values.  "
					+ "Even if the necessary classes are in the classpath, the services may still not be found "
					+ "if the jars do not include the necessary META-INF services folder.  "
					+ "For example, public-package-jars\\usace-rowcps-regi.jar contains implementation classes "
					+ "but not the service definitions.";
			logger.warning(mesg);
		}

		return hasCalcFlow && hasProjectChild;
	}

	private boolean hasGlobalService(Class klass) {
		GlobalServiceLoaderDelegate instance = GlobalServiceLoader.getInstance();
		ServiceLoader serviceLoader = instance.getServiceLoader(klass);

		return hasService(klass, serviceLoader);
	}

	private boolean hasService(Class klass, ServiceLoader sl) {
		boolean hasCalcFlow = false;
		ServiceLoader serviceLoader;

		GlobalServiceLoaderDelegate instance = GlobalServiceLoader.getInstance();
		serviceLoader = instance.getServiceLoader(klass);

		for (Object service : serviceLoader) {
			hasCalcFlow = true;
			break;
		}
		return hasCalcFlow;
	}

	public void generateStreamStatusImage(String officeId, String locationId, String templateName, Date current,
			final int width, final int height, String filename) throws DbConnectionException, DbIoException, IOException, InterruptedException, ExecutionException, TimeoutException {

		LocationTemplate locTemp = new LocationTemplate(officeId, locationId);

		AtLocationManager locMan = this.regiDomain.getAtLocationManager(getManagerId());
		Location loc = locMan.retrieveLocation(locTemp, CacheUsage.NORMAL);

		final TimeInfo ti = getTimeInfo(current, this.regiDomain.getTimeZone());

		TimeInfoSource tis = new TimeInfoSource() {
			@Override
			public hec.map.geoui.interp.TimeInfo getTimeInfo() {
				return ti;
			}
		};

		AtMapTemplateManager atMapTemplateManager = this.regiDomain.getAtMapTemplateManager(managerId);

		MapTemplateLayer mtl = getMapTemplateLayer(templateName);

		StreamGageGraphicOptionData sggod = mtl.getStreamGageGraphicOptions();
		final StreamData streamData = new StreamData(loc, sggod, tis, getManagerId());

		final CountDownLatch cdl = new CountDownLatch(1);
		PropertyChangeListener pcl = new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				if (GraphicConstants.DATA_FILLED_EVENT.equals(evt.getPropertyName())) {
					cdl.countDown();
				}
			}
		};

		streamData.addPropertyChangeListener(pcl);

//		logger.info("calling getCurrentEmbankment");
		streamData.getCurrentEmbankment();
//		logger.info("calling getCurrentFlow");
		streamData.getCurrentFlow();
//		logger.info("calling getCurrentStage");
		streamData.getCurrentStage();

		Integer seconds = Integer.getInteger(LATCH_SECONDS, 11 * 60); // This one goes to 11...
		boolean normalExit = cdl.await(seconds, TimeUnit.SECONDS);
		if (!normalExit) {
			logger.log(Level.INFO, "Exceeded timeout waiting for status data model to load.");
		}

		final Dimension d = new Dimension(width, height);

		String imageFormat = getFormatFromFile(filename);

		RunnableFuture<StreamPlotPanel> panelFuture = new FutureTask<StreamPlotPanel>(new Callable<StreamPlotPanel>() {
			@Override
			public StreamPlotPanel call() throws Exception {
				StreamPlotPanel spp = new StreamPlotPanel();  // Why is this building a panel on background thread?
				spp.setData(streamData);
				return spp;
			}
		}
		);

		SwingUtilities.invokeLater(panelFuture);

		final StreamPlotPanel panel = panelFuture.get(11, TimeUnit.MINUTES);

		layoutAndSave(panel, d, filename, imageFormat);
	}

	public void generateReleasesStatusImage(String officeId, String locationId, String templateName, Date current,
			final int width, final int height, String filename) throws DbConnectionException, DbIoException, IOException, InterruptedException, TimeoutException, ExecutionException {

		LocationTemplate locTemp = new LocationTemplate(officeId, locationId);

		AtLocationManager locMan = this.regiDomain.getAtLocationManager(getManagerId());
		Location loc = locMan.retrieveLocation(locTemp, CacheUsage.NORMAL);

		final TimeInfo ti = getTimeInfo(current, this.regiDomain.getTimeZone());

		TimeInfoSource tis = new TimeInfoSource() {
			@Override
			public hec.map.geoui.interp.TimeInfo getTimeInfo() {
				return ti;
			}
		};

		AtMapTemplateManager atMapTemplateManager = this.regiDomain.getAtMapTemplateManager(managerId);

		MapTemplateLayer mtl = getMapTemplateLayer(templateName);
		IMapTemplate mapTemplate = mtl.getMapTemplate();

//		List<ReleasesGraphicOptionData> rgods = mapTemplate.getGraphicOptionData(ReleasesGraphicOptionData.class);
		// rgods is empty
		ReleasesGraphicOptionData rgod = new ReleasesGraphicOptionData();

		Metrics metrics = MetricsServiceProvider.createMetrics(getClass().getSimpleName());
		final MyReleasesGraphicData data = new MyReleasesGraphicData(loc, managerId, tis, rgod, new OptionalParams(metrics));

		RunnableFuture<HeadlessReleasesGraphicPanel> rf = new FutureTask<HeadlessReleasesGraphicPanel>(new Callable<HeadlessReleasesGraphicPanel>() {
			@Override
			public HeadlessReleasesGraphicPanel call() throws Exception {
				HeadlessReleasesGraphicPanel releasesGraphicPanel = new HeadlessReleasesGraphicPanel();
				return releasesGraphicPanel;
			}
		});

		SwingUtilities.invokeLater(rf);
		HeadlessReleasesGraphicPanel gp = rf.get(11, TimeUnit.MINUTES);

		final HeadlessReleasesGraphicPanel releasesGraphicPanel = gp;

		final CountDownLatch dataFilledLatch = new CountDownLatch(1);
		PropertyChangeListener pcl = new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				if (GraphicConstants.DATA_FILLED_EVENT.equals(evt.getPropertyName())) {
					dataFilledLatch.countDown();
				}
			}
		};

		releasesGraphicPanel.setData(data);
		logger.info("Adding data PCL");
		data.addPropertyChangeListener(pcl);

		Thread.sleep(5000);

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
//                logger.info("Setting data in the graphic panel and firing data update request.");
				releasesGraphicPanel.fireDataUpdateRequest();
			}
		});

//        logger.info("Waiting 11 minutes on latch.");
		Integer seconds = Integer.getInteger(LATCH_SECONDS, 11 * 60);
		boolean normalExit = dataFilledLatch.await(seconds, TimeUnit.SECONDS);
		if (!normalExit) {
			logger.log(Level.WARNING, "Exceeded timeout waiting for releases status data to load.");
		}

		data.fireRepaintEvent();
		Thread.sleep(1000);

		RunnableFuture<BufferedImage> biFuture = new FutureTask<BufferedImage>(new Callable<BufferedImage>() {
			@Override
			public BufferedImage call() throws Exception {
				BufferedImage bImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
				Graphics2D g = bImage.createGraphics();

				Color color = new Color(255, 255, 255);
				g.setPaint(color);
				g.fillRect(0, 0, width, height);
				g.setColor(color);
				g.setClip(0, 0, width, height);

				final Dimension d = new Dimension(width, height);
				releasesGraphicPanel.setMinimumSize(d);
				releasesGraphicPanel.setMaximumSize(d);
				releasesGraphicPanel.setPreferredSize(d);
				releasesGraphicPanel.setSize(d);
				releasesGraphicPanel.setBounds(new Rectangle(0, 0, width, height));
//				releasesGraphicPanel.setDoubleBuffered(false);
//				layoutComponent(releasesGraphicPanel, d);
//				releasesGraphicPanel.show(true);
//				releasesGraphicPanel.setVisible(true);
				releasesGraphicPanel.paintImmediately(0, 0, width, height);

				releasesGraphicPanel.print(g);
				g.dispose();

				return bImage;
			}

		});

		SwingUtilities.invokeLater(biFuture);

		BufferedImage bImage = biFuture.get(11, TimeUnit.MINUTES);

//		Thread.sleep(5000);
//		releasesGraphicPanel.print(g);
		String imageFormat = getFormatFromFile(filename);
		File file = new File(filename);
		file.getParentFile().mkdirs();

		try (FileOutputStream fos = new FileOutputStream(file);
				BufferedOutputStream bos = new BufferedOutputStream(fos);) {
			logger.info("Writing releases image to output stream.");
			writeImage(imageFormat, 100.0f, bos, bImage);
		}

	}

	private void layoutComponent(JComponent component, Dimension d) {
		LayoutManager layout = component.getLayout();
		if (layout != null) {
			layout.layoutContainer(component);
		}
		component.setSize(d);
		component.addNotify();
		component.invalidate();
		component.validate();
		component.addNotify();
		if (layout != null) {
			layout.layoutContainer(component);
		}
	}

	public static class MyReleasesGraphicData extends ReleasesGraphicData {

		public MyReleasesGraphicData(Location loc, ManagerId manId, TimeInfoSource tis, ReleasesGraphicOptionData options, OptionalParams optionalParams) {
			super(loc, manId, tis, options, optionalParams);
		}

		@Override
		public void fireRepaintEvent() {
			super.fireRepaintEvent(); //To change body of generated methods, choose Tools | Templates.
		}

	}

	public TimeInfo getTimeInfo(Date current, TimeZone tz) {
		final int MILLIS_PER_HOUR = 1000 * 60 * 60;
		// The time controls internally use HecTimes with utc values and Regi formats them to the display timezone in the ui
		// components.
		// In headless the user is giving us a Date object and a timezone that date object is in.
		// So imagine the user wants a graphic displayed at midnight on 4/18 in CDT.  Say that UTC is ahead of CDT by 5 hours
		// so we need hectime to actually store the time at 5AM
		

		HecTime start = new HecTime(current, 0);

		Calendar endCal = new GregorianCalendar(tz);
		endCal.setTime(current);
		endCal.add(Calendar.DAY_OF_MONTH, 1);
		Date endDate = endCal.getTime();
		HecTime end = new HecTime(endDate, 0);

		Calendar curCal = new GregorianCalendar(tz);
		curCal.setTime(current);
		Date curDate = curCal.getTime();
		HecTime curTime = new HecTime(curDate, 0);
		curTime.showTimeAsBeginningOfDay(true);

		int stepSize = 1000 * 60 * 60;  // millisPerHour
		TimeInfo ti = new TimeInfo(start, end, curTime, stepSize);
		return ti;
	}

	private List<IMapTemplate> getMapTemplates() throws DbIoException, DbConnectionException {
		RegiDomain currentProject = (RegiDomain) RegiDomain.getCurrentProject();
		final AtMapTemplateManager tm = currentProject.getAtMapTemplateManager(getManagerId());
		final List<IMapTemplate> mapTemplates = tm.retrieveMapTemplates(currentProject.getUserOfficeId(),
				CacheUsage.NORMAL);
		return mapTemplates;
	}

	private List<MapTemplateLayer> getMapTemplateLayers(List<IMapTemplate> mapTemplates) {
		List<MapTemplateLayer> retval = new ArrayList<>();

		for (IMapTemplate iMapTemplate : mapTemplates) {
			retval.add(new MapTemplateLayer(iMapTemplate));
		}
		return retval;
	}

	private MapTemplateLayer getMapTemplateLayer(String templateName) throws DbIoException, DbConnectionException {
		MapTemplateLayer retval = null;

		List<IMapTemplate> mapTemplates = getMapTemplates();

		IMapTemplate matching = find(templateName, mapTemplates);

		if (matching != null) {
			retval = new MapTemplateLayer(matching);
		}

		return retval;
	}

	private IMapTemplate find(String templateName, List<IMapTemplate> mapTemplates) {
		IMapTemplate retval = null;

		if (mapTemplates != null && !mapTemplates.isEmpty() && templateName != null) {
			for (IMapTemplate mapTemplate : mapTemplates) {
				if (templateName.equalsIgnoreCase(mapTemplate.getName())) {
					retval = mapTemplate;
					break;
				}
			}
		}

		return retval;
	}

	public void generateBasinPieImage(final String officeId, final String locationStr, final String basinId,
			final Date date, final int width, final int height, final String template, final String file) throws Exception {

		generateBasinPieImages(officeId, new String[]{locationStr}, basinId, new Date[]{date}, width, height, new String[]{template}, file);
	}

	public void generateBasinPieImagesForBasin(final String officeId, final String basinId,
			final Date[] dates, final int width, final int height, final String[] templateIds, final String file) throws Exception {
//		BasinConnectivityDataAdapter connDA = new BasinConnectivityDataAdapter(new ManagerIdProvider() {
//			@Override
//			public ManagerId getManagerId() {
//				return managerId;
//			}
//		});
		BasinConnectivityDataAdapter connDA = BasinConnectivityDataAdapter.getInstance();

		// This also primes connDA for the getPrimary call..
		LocationGroup lg = buildLocationGroupWithAssignedLocations(connDA, basinId);
		Set<AssignedLocation> assignedLocations = lg.getAssignedLocations();

		List<LocationTemplate> locs = new ArrayList<>();
		for (AssignedLocation assignedLocation : assignedLocations) {
			locs.add(assignedLocation.getLocRef());
		}

		List<IChartTemplate> templates = getTemplates(templateIds, officeId);
		final Dimension d = new Dimension(width, height);
		String imageFormat = getFormatFromFile(file);

		generateImages(officeId, locs, basinId, dates, d, templates, file, imageFormat);
	}

	public void generateBasinPieImages(final String officeId, final String locationStrs[], final String basinId,
			final Date[] dates, final int width, final int height, final String[] templateIds, final String filename) throws Exception {
		System.setProperty("java.awt.headless", "true");
		if (width <= 0 || height <= 0) {  // is there a max?
			logger.warning("Width and Height parameters must be > 0");
			return;
		}

		List<IChartTemplate> templates = getTemplates(templateIds, officeId);

		List<LocationTemplate> locs = new ArrayList<>();
		for (String locationStr : locationStrs) {
			final LocationTemplate locRef = new LocationTemplate(officeId, locationStr);
			locs.add(locRef);
		}

		String imageFormat = getFormatFromFile(filename);
		final Dimension d = new Dimension(width, height);

		generateImages(officeId, locs, basinId, dates, d, templates, filename, imageFormat);
	}

	public void generateImages(final String officeId, List<LocationTemplate> locs, final String basinId, final Date[] dates, final Dimension d, List<IChartTemplate> templates, final String filePattern, String imageFormat) throws ExecutionException, DbConnectionException, InterruptedException {
		SortedSet<Date> dateSet = new TreeSet<>();
		dateSet.addAll(Arrays.asList(dates));

		Date startDate = dateSet.first();
		Date endDate = dateSet.last();

		BasinConnectivityDataAdapter connDA = BasinConnectivityDataAdapter.getInstance();

		// This also primes connDA for the getPrimary call..
		LocationGroup lg = buildLocationGroupWithAssignedLocations(connDA, basinId);

		boolean isBasin = true;
		LocationGroup projectsOnly = DecisionSupportEditor.getProjectOnlyLocationGroup(lg, connDA, isBasin);

		final OperationSupportBasinTreeModel treeModel = new OperationSupportBasinTreeModel(null);
		treeModel.fillBasinTree(lg, connDA);

		for (IChartTemplate chartTemplate : templates) {
			logger.info("Generating images for template:" + chartTemplate.getId());
			final BasinPieModel pieModel = buildAndInitializeBasinPieModel(projectsOnly, chartTemplate, startDate, endDate, treeModel);

			for (Date date : dateSet) {
				for (LocationTemplate locRef : locs) {
					String file = getFileName(date, filePattern, locRef.getLocationId(), imageFormat, chartTemplate.getIdSuffix(), officeId, basinId, d.width, d.height);
					drawImage(treeModel, locRef, pieModel, d, date, file, imageFormat);
				}
			}
		}
	}

	public List<IChartTemplate> getTemplates(final String[] templateIds, final String officeId) throws DbConnectionException, DbIoException {
		List<IChartTemplate> templates = new ArrayList<>();
		AtChartTemplateManager chartTemplateManager = regiDomain.getAtChartTemplateManager(managerId);
		for (String template : templateIds) {
			String templateId = IChartTemplate.CHART_TEMPLATE_CATEGORY_ID + "." + officeId + "." + template;

			IChartTemplate chartTemplate = chartTemplateManager.retrieveChartTemplate(officeId, templateId, CacheUsage.NORMAL);
			if (chartTemplate == null) {
				// try again using the user-provided string.
				chartTemplate = chartTemplateManager.retrieveChartTemplate(officeId, template, CacheUsage.NORMAL);
			}

			if (chartTemplate == null) {
				logger.warning("Could not locate chart:" + templateId);
			} else {
				templates.add(chartTemplate);
			}

		}
		return templates;
	}

	public static String getFileName(Date date, final String filePattern, final String locationStr, String imageFormat, String chartTemplate, String officeId, String basinId, int width, int height) {

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
//		String filename = officeId + "_" + locationStr + "_" + chartTemplate + "_" + dateStr;
//		Replace anything that isn't a-z or A-z or 0-9 or [:\/)(.-] with an underscore.
		filename = filename.replaceAll("[^a-zA-Z0-9:\\\\\\/\\)\\(\\.\\- ]", "_");

//		String file = dir + filename + "." + imageFormat;
//		String file =  filename + "." + imageFormat;
		return filename;
	}

	public static String getDateString(Date date) {
		Instant asInstant = date.toInstant();
		String dateStr = asInstant.toString();  // like: 2017-02-24T22:23:21.149Z
		dateStr = dateStr.replaceAll(":", "_");
		return dateStr;
	}

	public void drawImage(final OperationSupportBasinTreeModel treeModel, final LocationTemplate locRef, final BasinPieModel pieModel, final Dimension d, final Date date, final String file, final String imageFormat) throws InterruptedException, ExecutionException {

		final CountDownLatch latch = new CountDownLatch(1);
		final SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
			@Override
			protected Void doInBackground() throws Exception {
				List<LocationTemplate> relavantLocations = treeModel.getRelavantLocations(locRef);
				logger.log(Level.INFO, "Found {0} locations relevant to {1}", new Object[]{relavantLocations.size(), locRef});
				pieModel.setActiveLocations(relavantLocations, true);
				return null;
			}

			@Override
			protected void done() {
				try {
					logger.log(Level.INFO, "Creating Basin Pie Panel.");
					writeBasinImage(d, pieModel, date, file, imageFormat);
					latch.countDown();
				} catch (IOException ex) {
					logger.log(Level.SEVERE, null, ex);
				}
			}
		};

		worker.execute();

		Void got = worker.get();  // This returns as soon as doInBackground finishes.
		boolean normalExit = latch.await(Integer.getInteger(LATCH_SECONDS, 11 * 60), TimeUnit.SECONDS);
		if (!normalExit) {
			logger.log(Level.WARNING, "Exceeded timeout waiting for basin image to draw.");
		}

	}

	public BasinPieModel buildAndInitializeBasinPieModel(LocationGroup projectsOnly, final IChartTemplate chartTemplate, Date startDate, Date endDate, final OperationSupportBasinTreeModel treeModel) throws InterruptedException {
		IEventThreadExceptionProcessor eventThreadExceptionProcessor = null;
		final BasinPieModel pieModel = new BasinPieModel(managerId, eventThreadExceptionProcessor, projectsOnly, chartTemplate, startDate, endDate);
		final CountDownLatch initlatch = new CountDownLatch(1);
		pieModel.addPropertyChangeListener(new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				if ("PieDataChangedProperty".equals(evt.getPropertyName())) {
					logger.log(Level.FINER, "PieDataChanged edt:{0} latch:{1}",
							new Object[]{SwingUtilities.isEventDispatchThread(), initlatch.getCount()});
					initlatch.countDown();
				}
			}
		});
		List<LocationTemplate> forInitCache = treeModel.getRelavantLocations(null);
		logger.log(Level.INFO, "Initializing BasinPieModel with {0} locations.", forInitCache.size());
		pieModel.initCache(forInitCache);  // this can take a while but it fires a property change event when its done.

		boolean normalExit = initlatch.await(Integer.getInteger(LATCH_SECONDS, 11 * 60), TimeUnit.SECONDS);
		if (!normalExit) {
			logger.log(Level.WARNING, "Exceeded timeout waiting for basin pie model to load.");
		}

		return pieModel;
	}

	public LocationGroup buildLocationGroupWithAssignedLocations(BasinConnectivityDataAdapter connDA, String basinId) throws DbConnectionException {
		IBasin basin = findBasinById(basinId);

		LocationCategoryRef categoryRef = new LocationCategoryRef(AtBasinManager.BASIN_CATEGORY_REF_ID, basin.getOfficeId());
		final LocationGroup lg = new LocationGroup();//getLocationGroup(basin);
		lg.setName(basin.getBasinId());
		lg.setDbOfficeId(basin.getOfficeId());
		lg.setLocationGroupRef(new LocationGroupRef(categoryRef, basin.getOfficeId(), basin.getBasinId()));

		// This also primes connDA for the getPrimary call..
		List<IStreamLocation> streamLocations = connDA.getStreamLocations(basin);

		LocationTemplate assocLoc = new LocationTemplate(basin.getOfficeId(), basin.getBasinId(), null);
		Set<AssignedLocation> assLocs = new HashSet<>();

		AtomicInteger counter = new AtomicInteger(0);
		for (IStreamLocation loc : streamLocations) {
			String locationKind = connDA.getLocationKind(loc);
			LocationTemplate streamLocTemplate = new LocationTemplate(basin.getOfficeId(), loc.getLocationId(), null);
			AssignedLocation aLoc = new AssignedLocation(streamLocTemplate, loc.getLocationId(), 0, assocLoc);
			aLoc.setDescription("  (" + locationKind + ")");
			aLoc.setAttribute(counter.getAndIncrement());
			assLocs.add(aLoc);
		}

		lg.setAssignedLocations(assLocs);

		return lg;
	}

	private void writeBasinImage(Dimension d, final BasinPieModel pieModel, Date date, String file, String imageFormat) throws FileNotFoundException, IOException {

		final PiePanel piePanel = new PiePanel();

		TimeZone timezone = regiDomain.getTimeZone();
		piePanel.setTimeZone(timezone);

		JLayer piePanelJLayerWrapper = new JLayer(piePanel);
		BasinPieAnnotationLayer basinPieAnnotationLayer = new BasinPieAnnotationLayer();
		PinnableComponentGlassPane glassPane = PinnableComponentGlassPaneFactory.createNewGlassPane(basinPieAnnotationLayer, piePanel);

		piePanelJLayerWrapper.setGlassPane(glassPane);
		glassPane.addPinnableContainer(basinPieAnnotationLayer);
		PinnableContainer container = glassPane.getPinnableContainer(basinPieAnnotationLayer);
		basinPieAnnotationLayer.setPinnableContainer(container);
		container.setSize(d);

		PinnableComponentGlassPaneFactory.getGlassPane(basinPieAnnotationLayer).setVisible(true);
		
		basinPieAnnotationLayer.setChartTemplate(pieModel.getChartTemplate());
		basinPieAnnotationLayer.resetFromChartTemplate();
		
		logger.fine("Filling panel with model.");
		piePanel.fillPanel(pieModel);
		logger.info("Setting active date:" + date);
		piePanel.setActiveDate(date);

//		piePanel.getLayout().layoutContainer(piePanel);
//		piePanel.setSize(d);
//		piePanel.addNotify();
//		piePanel.invalidate();
//		piePanel.validate();
//		piePanel.addNotify();
//		piePanel.getLayout().layoutContainer(piePanel);
//
//		try (FileOutputStream fos = new FileOutputStream(file);
//				BufferedOutputStream bos = new BufferedOutputStream(fos);) {
//			logger.info("Writing to output stream");
//			saveToStream(bos, piePanel, imageFormat, 100.0f);
//		}		
		layoutAndSave(piePanelJLayerWrapper, d, file, imageFormat);
	}

	private IBasin findBasinById(String basinId) throws DbConnectionException {
		IBasin retval = null;
		AtBasinManager atBasinManager = regiDomain.getAtBasinManager(managerId);

		if (basinId != null && atBasinManager != null) {
			List<IBasin> allbasins = atBasinManager.retrieveAllBasins(CacheUsage.NORMAL);
			if (allbasins != null && !allbasins.isEmpty()) {
				for (IBasin basin : allbasins) {
					if (basinId.equals(basin.getBasinId())) {
						retval = basin;
						break;
					}
				}
			}
		}

		return retval;
	}

	/**
	 * Saves a copy of the plot as a image type
	 *
	 * @param os the output stream to write to.
	 * @param imageType i.e. "png" or "jpg".
	 * @param compression sets the compression for the image if the image writer supports it
	 */
	protected boolean saveToStream(OutputStream os, final JComponent panel, String imageType, float compression) throws IOException {
		// This next comment came from the source this was loosely based on.
		/**
		 * Add a revalidate here in order for headless scripting to work. Due to a joining of the building and displaying of the
		 * plot in showPlot(), any component changes to recompute preferred sizes and exports look bad and components overlap. If
		 * we do this here, then we can build and show the plot, tweak some properties and export correctly.
		 */

		BufferedImage bImage;

		if (SwingUtilities.isEventDispatchThread()) {
			bImage = saveToImage(panel.getSize(), panel);
		} else {
			Callable<BufferedImage> imageCallable = new Callable<BufferedImage>() {
				@Override
				public BufferedImage call() throws Exception {
					return saveToImage(panel.getSize(), panel);
				}
			};
			FutureTask<BufferedImage> futureTask = new FutureTask<>(imageCallable);

			SwingUtilities.invokeLater(futureTask);
			try {
				// Not sure how long to wait here.  Infinite is wrong.
				bImage = futureTask.get(5, TimeUnit.MINUTES);
			} catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
				throw new IOException("Background paint was interrupted.", ex);
			} catch (ExecutionException ex) {
				throw new IOException("Background paint encountered ExecutionException.", ex);
			} catch (TimeoutException ex) {
				throw new IOException("Timeout waiting for paint to complete.", ex);
			}
		}
		return writeImage(imageType, compression, os, bImage);
	}

	public boolean writeImage(String imageType, float compression, OutputStream os, BufferedImage bImage) {
		Iterator<ImageWriter> iter = javax.imageio.ImageIO.getImageWritersByFormatName(imageType);
		if (!iter.hasNext()) {
			logger.log(Level.WARNING, "No Image writers exist for Image Type = {0}", imageType);
			return true;
		}
		ImageWriter next = iter.next();
		ImageWriteParam defaultWriteParam = next.getDefaultWriteParam();
		if (defaultWriteParam.canWriteCompressed() && compression != hec.lang.Const.UNDEFINED_INT) {
			defaultWriteParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
			defaultWriteParam.setCompressionQuality(compression / 100);
		}
		writeToStream(next, os, bImage, defaultWriteParam);
		return false;
	}

	public static BufferedImage saveToImage(Dimension d, JComponent panel) {

		BufferedImage bImage = new BufferedImage(d.width, d.height, BufferedImage.TYPE_INT_RGB);
		paintIntoImage(bImage, d, panel);

		return bImage;
	}

	private static void paintIntoImage(BufferedImage bImage, Dimension d, JComponent panel) {
		Graphics2D g = bImage.createGraphics();

		boolean useTrans = false;

		if (!useTrans) {
			Color color = new Color(226, 226, 226);  // make backgroup grey
			g.setColor(color);
			g.fillRect(0, 0, d.width, d.height);
		} else {
			// This doesn't work.  
			Color color = new Color(0, 0, 0, 0);
			Composite composite = g.getComposite();
			g.setColor(color);
			g.setComposite(AlphaComposite.Clear);
			g.fillRect(0, 0, d.width, d.height);
			g.setComposite(AlphaComposite.SrcOver);
		}

		panel.paint(g);
		g.dispose();
	}

	public static void writeToStream(ImageWriter writer, OutputStream fs, BufferedImage bImage,
			ImageWriteParam defaultWriteParam) {
		try {
			writer.setOutput(javax.imageio.ImageIO.createImageOutputStream(fs));
			writer.write(null, new IIOImage(bImage, null, null), defaultWriteParam);
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Exception encountered writing image.", e);
		} finally {
			writer.dispose();
			try {
				if (fs != null) {
					fs.flush();
					fs.close();
				}
			} catch (IOException ioe) {
				logger.log(Level.SEVERE, "IOException encountered while closing OutputStream.", ioe);
			}
		}
	}

	protected String getFormatFromFile(String file) {
		String format = "png";

		if (file != null && !file.isEmpty()) {
			String asLower = file.toLowerCase();

			if (asLower.endsWith(".jpg") || asLower.endsWith(".jpeg")) {
				return "jpg";
			}

		}

		return format;
	}

	public void layoutAndSave(final JComponent component, final Dimension d, String file, String imageFormat) throws IOException {
		if (SwingUtilities.isEventDispatchThread()) {
			layoutComponent(component, d);
		} else {
			Callable<Boolean> imageCallable = new Callable<Boolean>() {
				@Override
				public Boolean call() throws Exception {
					layoutComponent(component, d);
					return Boolean.TRUE;
				}
			};
			FutureTask<Boolean> futureTask = new FutureTask<>(imageCallable);

			SwingUtilities.invokeLater(futureTask);
			try {
				// Not sure how long to wait here.  Infinite is wrong.
				Boolean dontcare = futureTask.get(1, TimeUnit.MINUTES);
			} catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
				throw new IOException("Background layout was interrupted.", ex);
			} catch (ExecutionException ex) {
				throw new IOException("Background layout encountered ExecutionException.", ex);
			} catch (TimeoutException ex) {
				throw new IOException("Timeout waiting for layout to complete.", ex);
			}
		}

		File f = new File(file);
		f.getParentFile().mkdirs();

		try (FileOutputStream fos = new FileOutputStream(f);
				BufferedOutputStream bos = new BufferedOutputStream(fos);) {
			logger.info("Writing to output stream");
			saveToStream(bos, component, imageFormat, 100.0f);
		}
	}

}
