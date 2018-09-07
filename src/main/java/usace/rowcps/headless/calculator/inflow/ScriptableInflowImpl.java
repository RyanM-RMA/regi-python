package usace.rowcps.headless.calculator.inflow;

import hec.data.DataSetException;
import hec.data.DataSetIllegalArgumentException;
import hec.data.Interval;
import hec.data.location.LocationTemplate;
import hec.data.project.AtProjectDescriptor;
import hec.db.DbConnectionException;
import hec.db.DbException;
import hec.db.DbIoException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TimeZone;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import usace.metrics.services.Metrics;
import usace.metrics.services.MetricsServiceProvider;
import usace.rowcps.computation.common.IEventThreadExceptionProcessor;
import usace.rowcps.computation.common.IntervalProvider;
import usace.rowcps.computation.inflow.AutoAdjustInflowsAction;
import usace.rowcps.computation.inflow.BalanceAdjustedInflowsAction;
import usace.rowcps.computation.inflow.CloneInflowsAction;
import usace.rowcps.computation.inflow.InflowAdjustedTypeModel;
import usace.rowcps.computation.inflow.InflowCache;
import usace.rowcps.computation.inflow.InflowComputation;
import usace.rowcps.computation.inflow.ZeroNegativeAdjustedInflowsAction;
import usace.rowcps.data.CacheInitializationException;
import usace.rowcps.data.LocalOffset;
import usace.rowcps.headless.calculator.AbstractScriptableCalc;
import usace.rowcps.headless.interfaces.ScriptableCalc;
import usace.rowcps.regi.event.IThreadedBlockRetriever;
import usace.rowcps.regi.executor.DefaultThreadIdProvider;
import usace.rowcps.regi.executor.ThreadIdProvider;
import usace.rowcps.regi.interfaces.model.ICurrentDayControl;
import usace.rowcps.regi.model.CacheUsage;
import usace.rowcps.regi.model.ManagerId;
import usace.rowcps.regi.model.OptionalParams;
import usace.rowcps.regi.model.RegiDomain;
import usace.rowcps.regi.status.AtProjectManager;
//import usace.rowcps.regi.util.RowcpsFutureDescriptor;

/**
 *
 * @author ryan
 */
public class ScriptableInflowImpl extends AbstractScriptableCalc implements ScriptableCalc, ScriptableInflow {

    private static final Logger LOGGER = Logger.getLogger(ScriptableInflowImpl.class.getName());

    private final long _msTimeOffsetIntoInterval = 7 * 3600 * 1000;  //TODO  7am for now
    private final String _intervalName = "1Day";

    private final Map<LocationTemplate, InflowAdjustedTypeModel> statusMaps = new HashMap<>();

    private IntervalProvider buildIntervalProvider(TimeZone projectTimeZone) {
    return new IntervalProvider() {
        @Override
        public boolean isPeriodAverage() {
//                return _periodAverageFlows;
            return false;
        }          

        @Override
        public Interval getInterval() {
            Interval interval = null;
            try {
                if (_intervalName == null) {
                    interval = new Interval("1Day");
                } else {
                    interval = new Interval(_intervalName);
                }
            } catch (DataSetIllegalArgumentException ex) {
                LOGGER.log(Level.WARNING,
                        "Error instantiating interval for: " + _intervalName + ".",
                        ex);
            }

            return interval;
        }

        @Override
        public int getIntervalOffsetSeconds() {
            return (int) (_msTimeOffsetIntoInterval / 1000L);
        }

        @Override
        public int getUtcIntervalOffsetSeconds() {
            LocalOffset localOffset = new LocalOffset(projectTimeZone, getInterval());
				return localOffset.getUtcOffsetInSeconds();
        }
    };

    }
    public ScriptableInflowImpl(RegiDomain regiDomain, ManagerId managerId) {
        super(regiDomain, managerId);
    }

    @Override
    public void autoAdjust(String officeId, String locationStr, Date startDate) {
        autoAdjust(officeId, locationStr, startDate, false, false);
    }

    @Override
    public void autoAdjust(String officeId, String locationStr, Date startDate, boolean useLimits, boolean freezeRain) {
        Metrics metrics = MetricsServiceProvider.createMetrics(this.getClass().getSimpleName(), "autoAdjust");
        OptionalParams options = new OptionalParams(metrics);
        try {
            LocationTemplate locRef = new LocationTemplate(officeId, locationStr);

            InflowAdjustedTypeModel asm = getOrCreateStatusMap(locRef);

            InflowCache inflowCache = getCache(locRef, startDate, options);
            startDate = inflowCache.getCeilingDateKey(startDate);

            LOGGER.info("performing AutoAdjustInflowsAction");

            AutoAdjustInflowsAction aaia = new AutoAdjustInflowsAction(startDate, inflowCache, hec.data.Units.ENGLISH_ID, asm);
            aaia.setFreezeRainDays(freezeRain);
            aaia.setUseLimits(useLimits);

            aaia.actionPerformed(null);
            LOGGER.info("AutoAdjustInflowsAction complete. Saving cache data.");

            inflowCache.saveData(options);

        } catch (DbConnectionException ex) {
            Logger.getLogger(ScriptableInflowImpl.class.getName()).log(Level.SEVERE, null, ex);
        } catch (DbIoException ex) {
            Logger.getLogger(ScriptableInflowImpl.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        } catch (Exception ex) {
            Logger.getLogger(ScriptableInflowImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void cloneInflows(String officeId, String locationStr, Date startDate) {
        Metrics metrics = MetricsServiceProvider.createMetrics(this.getClass().getSimpleName(), "cloneInflows");
        OptionalParams options = new OptionalParams(metrics);

        try {
            LocationTemplate locRef = new LocationTemplate(officeId, locationStr);

            InflowAdjustedTypeModel asm = getOrCreateStatusMap(locRef);

            InflowCache inflowCache = getCache(locRef, startDate, options);
            startDate = inflowCache.getCeilingDateKey(startDate);

            LOGGER.info("performing CloneInflowsAction");
            CloneInflowsAction cloneAction
                    = new CloneInflowsAction(startDate, inflowCache, hec.data.Units.ENGLISH_ID, asm);
            cloneAction.actionPerformed(null);
            LOGGER.info("CloneInflowsAction completed. Saving cache data.");

            inflowCache.saveData(options);

        } catch (DbConnectionException ex) {
            Logger.getLogger(ScriptableInflowImpl.class.getName()).log(Level.SEVERE, null, ex);
        } catch (DbIoException ex) {
            Logger.getLogger(ScriptableInflowImpl.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(ScriptableInflowImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void zeroNegatives(String officeId, String locationStr, Date startDate) {
        Metrics metrics = MetricsServiceProvider.createMetrics(this.getClass().getSimpleName(), "zeroNegatives");
        OptionalParams options = new OptionalParams(metrics);

        try {
            LocationTemplate locRef = new LocationTemplate(officeId, locationStr);

            InflowAdjustedTypeModel asm = getOrCreateStatusMap(locRef);

            InflowCache inflowCache = getCache(locRef, startDate, options);
            startDate = inflowCache.getCeilingDateKey(startDate);

            LOGGER.info("performing ZeroNegativeAdjustedInflowsAction");
            ZeroNegativeAdjustedInflowsAction zeroAction = new ZeroNegativeAdjustedInflowsAction(startDate, inflowCache,
                    hec.data.Units.ENGLISH_ID, asm);
            zeroAction.actionPerformed(null);
            LOGGER.info("ZeroNegativeAdjustedInflowsAction complete.  Saving cache data.");
            inflowCache.saveData(options);

        } catch (DbConnectionException ex) {
            Logger.getLogger(ScriptableInflowImpl.class.getName()).log(Level.SEVERE, null, ex);
        } catch (DbIoException ex) {
            Logger.getLogger(ScriptableInflowImpl.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(ScriptableInflowImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void balanceAll(String officeId, String locationStr, Date startDate) {
        Metrics metrics = MetricsServiceProvider.createMetrics(this.getClass().getSimpleName(), "balanceAll");
        OptionalParams options = new OptionalParams(metrics);
        try {
            LocationTemplate locRef = new LocationTemplate(officeId, locationStr);

            InflowCache inflowCache = getCache(locRef, startDate, options);
            startDate = inflowCache.getCeilingDateKey(startDate);

            InflowAdjustedTypeModel asm = getOrCreateStatusMap(locRef);

            LOGGER.info("performing BalanceAdjustedInflowsAction");
            BalanceAdjustedInflowsAction balanceAll = new BalanceAdjustedInflowsAction(startDate, inflowCache,
                    hec.data.Units.ENGLISH_ID, asm);
            balanceAll.actionPerformed(null);
            LOGGER.info("BalanceAdjustedInflowsAction complete.  Saving data.");

            inflowCache.saveData(options);

        } catch (DbConnectionException ex) {
            Logger.getLogger(ScriptableInflowImpl.class.getName()).log(Level.SEVERE, null, ex);
        } catch (DbIoException ex) {
            Logger.getLogger(ScriptableInflowImpl.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(ScriptableInflowImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private InflowCache getCache(LocationTemplate locRef, final Date startDate, OptionalParams options)
            throws DbConnectionException, DbIoException,
            InterruptedException, CacheInitializationException, DbException, DataSetException
	{
        RegiDomain domain = getRegiDomain();
        AtProjectManager atProjectManager = domain.getAtProjectManager(getManagerId());
        AtProjectDescriptor projectDescriptor = atProjectManager.getProjectDescriptor(locRef, CacheUsage.NORMAL);
        IEventThreadExceptionProcessor eventThreadExceptionProcessor = null;

        // This seems like a really big hack.  Is this really needed?
        final CountDownLatch headLatch = new CountDownLatch(1);
        IThreadedBlockRetriever completionCallbackTarget = new AbstractThreadedBlockRetriever() {

            @Override
            public void asyncHeadCacheFetchCompleted() {
                LOGGER.info("asyncHeadCacheFetchCompleted");
                headLatch.countDown();
            }
        };
        
        Set<Date> modifiedDatesForCachedSettings = null;

        TimeZone projectTimeZone = atProjectManager.getIProject(projectDescriptor).getProjectTimeZone();
        ICurrentDayControl currentDayControl = new HeadlessInflowCurrentDayControl(managerId, startDate, projectTimeZone, locRef);
		
        InflowCache inflowCache = new InflowCache(currentDayControl, getManagerId(), projectDescriptor,
                eventThreadExceptionProcessor, completionCallbackTarget,
                modifiedDatesForCachedSettings, projectTimeZone, getIntervalProvider(projectTimeZone)) {

            @Override
            protected void updateHeadTimeWindow(Calendar start, Calendar end, SortedMap<Date, ? extends Object> currentCache) {
                super.updateHeadTimeWindow(start, end, currentCache);
                if (start.getTime().after(startDate)) {
                    start.setTime(startDate);
                }
            }
        };

	    ThreadIdProvider idprov = new DefaultThreadIdProvider(); // new RegiTabSpec(RegiTabType.INFLOW));
        inflowCache.initCache( idprov, options);

        LOGGER.info("Waiting for InflowCache to initialize.");
		Integer seconds = Integer.getInteger("rowcps.latchseconds", 11*60);
		headLatch.await(seconds, TimeUnit.SECONDS);        

        /*
        inflowCache.appendDataToHeadOfCache(futureMap, options);
        // This needs more thought.  Peter thinks db timesout at 10 minutes.
        // Needs to be a value higher than any user would be willing to wait.
        for (Map.Entry<RowcpsFutureDescriptor, Object> entry : futureMap.entrySet()) {
            RowcpsFutureDescriptor desc = entry.getKey();
            Object value = entry.getValue();
            Future future = desc.getFuture();
            try {
                future.get(10000, TimeUnit.MILLISECONDS);
            } catch (ExecutionException ex) {
                Logger.getLogger(ScriptableInflowImpl.class.getName()).log(Level.SEVERE, null, ex);
            } catch (TimeoutException ex) {
                Logger.getLogger(ScriptableInflowImpl.class.getName()).log(Level.SEVERE, null, ex);
            }
            System.out.println("asdf");
        }
        */
        
        final TimeZone tz = inflowCache.getProjectTimeZone();
        List<Date> datesInMonth = InflowComputation.getDatesInMonth(startDate, tz);
        for (Date dateKey : datesInMonth) {
            inflowCache.computedInflowForDate(dateKey, hec.data.Units.ENGLISH_ID, tz);
            inflowCache.computeInflowMassDelta(dateKey, hec.data.Units.ENGLISH_ID);
        }

        LOGGER.info("InflowCache is initialized.");
        return inflowCache;
    }

    private IntervalProvider getIntervalProvider(TimeZone projectTimeZone) {
        return buildIntervalProvider(projectTimeZone);
    }

    private InflowAdjustedTypeModel getOrCreateStatusMap(LocationTemplate template) {
        InflowAdjustedTypeModel retval = null;
        if (template != null) {
            retval = statusMaps.get(template);
            if (retval == null) {
                retval = new InflowAdjustedTypeModelImpl();
                statusMaps.put(template, retval);
            }
        }

        return retval;
    }
}
