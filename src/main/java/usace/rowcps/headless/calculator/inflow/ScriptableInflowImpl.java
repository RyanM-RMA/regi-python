package usace.rowcps.headless.calculator.inflow;

import hec.data.DataSetIllegalArgumentException;
import hec.data.Interval;
import hec.data.location.LocationTemplate;
import hec.db.DbConnectionException;
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
import usace.rowcps.computation.common.IThreadedBlockRetriever;
import usace.rowcps.computation.common.IntervalProvider;
import usace.rowcps.computation.inflow.AutoAdjustInflowsAction;
import usace.rowcps.computation.inflow.BalanceAdjustedInflowsAction;
import usace.rowcps.computation.inflow.CloneInflowsAction;
import usace.rowcps.computation.inflow.InflowAdjustedTypeModel;
import usace.rowcps.computation.inflow.InflowCache;
import usace.rowcps.computation.inflow.InflowComputation;
import usace.rowcps.computation.inflow.ZeroNegativeAdjustedInflowsAction;
import usace.rowcps.data.CacheInitializationException;
import usace.rowcps.data.project.AtProjectDescriptor;
import usace.rowcps.headless.calculator.AbstractScriptableCalc;
import usace.rowcps.headless.interfaces.ScriptableCalc;
import usace.rowcps.regi.interfaces.model.ICurrentDayControl;
import usace.rowcps.regi.model.AtProjectManager;
import usace.rowcps.regi.model.CacheUsage;
import usace.rowcps.regi.model.ManagerId;
import usace.rowcps.regi.model.OptionalParams;
import usace.rowcps.regi.model.RegiDomain;
import usace.rowcps.regi.util.RowcpsFutureDescriptor;

/**
 *
 * @author ryan
 */
public class ScriptableInflowImpl extends AbstractScriptableCalc implements ScriptableCalc, ScriptableInflow {

    private static final Logger logger = Logger.getLogger(ScriptableInflowImpl.class.getName());

    private long _msTimeOffsetIntoInterval = 7 * 3600 * 1000;  //TODO  7am for now
    private String _intervalName = "1Day";

    private Map<LocationTemplate, InflowAdjustedTypeModel> statusMaps = new HashMap<>();

    private IntervalProvider intervalProvider = new IntervalProvider() {
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
                logger.log(Level.WARNING,
                        "Error instantiating interval for: " + _intervalName + ".",
                        ex);
            }

            return interval;
        }

        @Override
        public int getIntervalOffsetSeconds() {
            return (int) (_msTimeOffsetIntoInterval / 1000L);
        }
    };

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

            logger.info("performing AutoAdjustInflowsAction");

            AutoAdjustInflowsAction aaia = new AutoAdjustInflowsAction(startDate, inflowCache, hec.data.Units.ENGLISH_ID, asm);
            aaia.setFreezeRainDays(freezeRain);
            aaia.setUseLimits(useLimits);

            aaia.actionPerformed(null);
            logger.info("AutoAdjustInflowsAction complete. Saving cache data.");

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

            logger.info("performing CloneInflowsAction");
            CloneInflowsAction cloneAction
                    = new CloneInflowsAction(startDate, inflowCache, hec.data.Units.ENGLISH_ID, asm);
            cloneAction.actionPerformed(null);
            logger.info("CloneInflowsAction completed. Saving cache data.");

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

            logger.info("performing ZeroNegativeAdjustedInflowsAction");
            ZeroNegativeAdjustedInflowsAction zeroAction = new ZeroNegativeAdjustedInflowsAction(startDate, inflowCache,
                    hec.data.Units.ENGLISH_ID, asm);
            zeroAction.actionPerformed(null);
            logger.info("ZeroNegativeAdjustedInflowsAction complete.  Saving cache data.");
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

            logger.info("performing BalanceAdjustedInflowsAction");
            BalanceAdjustedInflowsAction balanceAll = new BalanceAdjustedInflowsAction(startDate, inflowCache,
                    hec.data.Units.ENGLISH_ID, asm);
            balanceAll.actionPerformed(null);
            logger.info("BalanceAdjustedInflowsAction complete.  Saving data.");

            inflowCache.saveData(options);

        } catch (DbConnectionException ex) {
            Logger.getLogger(ScriptableInflowImpl.class.getName()).log(Level.SEVERE, null, ex);
        } catch (DbIoException ex) {
            Logger.getLogger(ScriptableInflowImpl.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(ScriptableInflowImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public InflowCache getCache(LocationTemplate locRef, final Date startDate, OptionalParams options)
            throws DbConnectionException, DbIoException,
            InterruptedException, CacheInitializationException {
        RegiDomain domain = getRegiDomain();
        AtProjectManager atProjectManager = domain.getAtProjectManager(getManagerId());
        AtProjectDescriptor projectDescriptor = atProjectManager.getProjectDescriptor(locRef, CacheUsage.NORMAL);
        IEventThreadExceptionProcessor eventThreadExceptionProcessor = null;

        // This seems like a really big hack.  Is this really needed?
        final CountDownLatch headLatch = new CountDownLatch(1);
        final CountDownLatch tailLatch = new CountDownLatch(1);
        IThreadedBlockRetriever completionCallbackTarget = new AbstractThreadedBlockRetriever() {

            @Override
            public void asyncHeadCacheFetchCompleted() {
                logger.info("asyncHeadCacheFetchCompleted");
                headLatch.countDown();
            }

            @Override
            public void asyncTailCacheFetchCompleted() {
                logger.info("asyncTailCacheFetchCompleted");
                tailLatch.countDown();
            }

        };

        Set<Date> modifiedDatesForCachedSettings = null;

        ICurrentDayControl currentDayControl = new ICurrentDayControl() {

            @Override
            public void setDate(Date date, boolean fireEvents) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public Date getCurrentDate() {
                return startDate;
            }

            @Override
            public int getLookbackDays() {
                return 31;
            }

            @Override
            public int getLookForwardDays() {
                return 31;
            }
        };

        InflowCache inflowCache = new InflowCache(currentDayControl, getManagerId(), projectDescriptor,
                eventThreadExceptionProcessor, completionCallbackTarget,
                modifiedDatesForCachedSettings, getIntervalProvider()) {

            @Override
            protected void updateHeadTimeWindow(Calendar start, Calendar end, SortedMap<Date, ? extends Object> currentCache) {
                super.updateHeadTimeWindow(start, end, currentCache);
                if (start.getTime().after(startDate)) {
                    start.setTime(startDate);
                }
            }

        };

        HashMap<RowcpsFutureDescriptor, Object> futureMap = new HashMap<RowcpsFutureDescriptor, Object>();
        inflowCache.initCache( futureMap, options);

        logger.info("Waiting for InflowCache to initialize.");
        headLatch.await(11, TimeUnit.MINUTES);  // This one goes to 11...
        tailLatch.await(11, TimeUnit.MINUTES);  // This one goes to 11...

//        inflowCache.appendDataToHeadOfCache(futureMap, options);
//        inflowCache.appendDataToTailOfCache(futureMap, options);
        // This needs more thought.  Peter thinks db timesout at 10 minutes.
        // Needs to be a value higher than any user would be willing to wait.
//         for (Map.Entry<RowcpsFutureDescriptor, Object> entry : futureMap.entrySet()) {
//            RowcpsFutureDescriptor desc = entry.getKey();
//            Object value = entry.getValue();
//            Future future = desc.getFuture();
//            try {
//                future.get(10000, TimeUnit.MILLISECONDS);
//            } catch (ExecutionException ex) {
//                Logger.getLogger(ScriptableInflowImpl.class.getName()).log(Level.SEVERE, null, ex);
//            } catch (TimeoutException ex) {
//                Logger.getLogger(ScriptableInflowImpl.class.getName()).log(Level.SEVERE, null, ex);
//            }
//            System.out.println("asdf");
//        }
        logger.info("InflowCache is initialized.");

        final TimeZone tz = inflowCache.getProjectTimeZone();
        List<Date> datesInMonth = InflowComputation.getDatesInMonth(startDate, tz);
        for (Date dateKey : datesInMonth) {
            inflowCache.computedInflowForDate(dateKey, hec.data.Units.ENGLISH_ID, tz);
        }

        for (Date dateKey : datesInMonth) {
            inflowCache.computeInflowMassDelta(dateKey, hec.data.Units.ENGLISH_ID);
        }

        logger.info("InflowCache is initialized.");
        return inflowCache;
    }

    public IntervalProvider getIntervalProvider() {
        return intervalProvider;
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
