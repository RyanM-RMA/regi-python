/*
* To change this license header, choose License Headers in Project Properties.
* To change this template file, choose Tools | Templates
* and open the template in the editor.
*/
package usace.rowcps.headless.sigstages.retrieve;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import usace.metrics.services.Timer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import usace.metrics.services.Metrics;
import usace.metrics.services.MetricsServiceProvider;
import usace.rowcps.headless.interfaces.ScriptableCalc;
import usace.rowcps.headless.sigstages.retrieve.xmlmodel.Action;
import usace.rowcps.headless.sigstages.retrieve.xmlmodel.Bankfull;
import usace.rowcps.headless.sigstages.retrieve.xmlmodel.Flood;
import usace.rowcps.headless.sigstages.retrieve.xmlmodel.Low;
import usace.rowcps.headless.sigstages.retrieve.xmlmodel.Major;
import usace.rowcps.headless.sigstages.retrieve.xmlmodel.Moderate;
import usace.rowcps.headless.sigstages.retrieve.xmlmodel.Sigstage;
import usace.rowcps.headless.sigstages.retrieve.xmlmodel.Sigstages;
import usace.rowcps.headless.sigstages.retrieve.xmlmodel.Site;
import usace.rowcps.regi.level.importer.LocationLevelRow;
import usace.rowcps.regi.model.RegiDomain;
import usace.rowcps.regi.model.ManagerId;

/**
 *
 * @author stephen
 */
public class RetrieveSigStagesImpl implements RetrieveSigstages, ScriptableCalc
{
    private RegiDomain _regiDomain;
    private ManagerId _managerId;
    
    private JAXBContext _jaxbContext = null;
    
    public final static String DOWNLOADURL = System.getProperty("sigstages.downloadurl", "http://water.weather.gov/ahps2/hydrograph_to_xml.php?gage=GAGENAME&output=xml");
    public static final String DELIMETER = System.getProperty("sigstages.delim", ";");
    public static final String CSVDELIMETER = System.getProperty("sigstages.csv.delim", "\n");
    public static final String CSVSEPARATOR = System.getProperty("sigstages.csv.separator", ",");
    public static int THREADCOUNT = 4;
    
    private String _parameter;
    private String _parameterType;
    private String _duration;
    private Map<Sigstage.Type, String> _specifiedLevelOverride;
    private String _office;
    private String _csvHeader;
    
    protected RetrieveSigStagesImpl()
    {
        super();
        _parameter = "Stage";
        _parameterType = "Inst";
        _duration = "0";
        _specifiedLevelOverride = new HashMap<>();
        _office = "SWF";
        _csvHeader = "Office,Location Level,Effective Date,Constant Level,Unit,Seasonal Value,Interpolate,Calendar Interval,Time Interval,Origin Date,Calendar Offset,TSID\n";
        String threadCount = System.getProperty("sigstages.threadcount", Runtime.getRuntime().availableProcessors()+"").trim();
        if(threadCount.matches("^[\\d]+$"))
        {
            THREADCOUNT = Integer.parseInt(threadCount);
        }
    }
    
    public void setCSVHeader(String csvHeader)
    {
        //TODO: Be sure this actually is a valid header
        _csvHeader = csvHeader;
    }
    
    public String getCSVHeader()
    {
        return _csvHeader;
    }
    
    public RetrieveSigStagesImpl(RegiDomain regiDomain, ManagerId managerId)
    {
        this();
        _regiDomain = regiDomain;
        _managerId = managerId;
    }
    
    @Override
    public void retrieveSigstages(String sourceFile, String outputFile)
    {
        Path inputPath = Paths.get(sourceFile);
        Path outputPath = Paths.get(outputFile);
        retrieveSigstages(inputPath, outputPath);
    }
    
    public void retrieveSigstages(Path source, Path outputPath)
    {
        SigstageLocation[] locations;
        try
        {
            locations = readLocationFile(source);
            fetchSitesThreaded(locations);
            locationsToCSV(outputPath, locations);
        }
        catch (IOException ex)
        {
            Logger.getLogger(RetrieveSigStagesImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void locationsToCSV(Path file, SigstageLocation... locations)
    {
        Metrics metrics = MetricsServiceProvider.createMetrics("locationsToCSV");
        try(Timer.Context context = metrics.createTimer().start())
        {
            
            StringBuilder csvBuilder = new StringBuilder(getCSVHeader());
            for(int locationIndex = 0; locationIndex < locations.length; locationIndex++)
            {
                if(locations[locationIndex] == null)
                {
                    continue;
                }
                if(locations[locationIndex].getSite() == null)
                {
                    continue;
                }
                Site site = locations[locationIndex].getSite();
                
                if(site.getSigstages() == null)
                {
                    continue;
                }
                Sigstages sigstages = site.getSigstages();
                
                List<Sigstage> allStages = new ArrayList<>();
                
                if(sigstages.getAction() == null)
                {
                    continue;
                }
                Action action = sigstages.getAction();
                allStages.add(action);
                
                Bankfull bankfull = sigstages.getBankfull();
                allStages.add(bankfull);
                
                Flood flood = sigstages.getFlood();
                allStages.add(flood);
                
                Low low = sigstages.getLow();
                allStages.add(low);
                
                Major major = sigstages.getMajor();
                allStages.add(major);
                
                //Record record = sigstages.getRecord();
                //allStages.add(record);
                
                Moderate moderate = sigstages.getModerate();
                allStages.add(moderate);
                
                for(int stageIndex = 0; stageIndex < allStages.size(); stageIndex++)
                {
                    Sigstage stage = allStages.get(stageIndex);
                    if(stage.getValue() == 0)
                    {
                        continue;
                    }
                    String dateTime = "";
                    if(site.getGenerationtime() != null && !site.getGenerationtime().isEmpty())
                    {
                        ZonedDateTime generationDateTime = ZonedDateTime.parse(site.getGenerationtime(), DateTimeFormatter.ISO_DATE_TIME);
                        dateTime = LocationLevelRow.DATE_TIME_FORMATTER.format(generationDateTime);
                    }
                    csvBuilder.append(_office);
                    csvBuilder.append(CSVSEPARATOR);
                    csvBuilder.append(stage.getType().generateID(locations[locationIndex].getOriginal(), _parameter, _parameterType, _duration, getSpecifiedLevelOverride(stage.getType())));
                    csvBuilder.append(CSVSEPARATOR);
                    csvBuilder.append(dateTime);
                    csvBuilder.append(CSVSEPARATOR);
                    csvBuilder.append(stage.getValue());
                    csvBuilder.append(CSVSEPARATOR);
                    csvBuilder.append(stage.getUnits());
                    csvBuilder.append(CSVSEPARATOR);
                    csvBuilder.append(CSVSEPARATOR);
                    csvBuilder.append(CSVSEPARATOR);
                    csvBuilder.append(CSVSEPARATOR);
                    csvBuilder.append(CSVSEPARATOR);
                    csvBuilder.append(CSVSEPARATOR);
                    csvBuilder.append(CSVSEPARATOR);
                    csvBuilder.append(CSVDELIMETER);
                }
            }
            
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file.toFile())))
            {
                writer.write(csvBuilder.toString());
            }
            catch (IOException ex)
            {
                Logger.getLogger(RetrieveSigStagesImpl.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

//    public void fetchSites(Location... locationNames)
//    {
//        for(int locationNamesIndex = 0; locationNamesIndex < locationNames.length; locationNamesIndex++)
//        {
//            String fullURL = DOWNLOADURL.replace("GAGENAME", locationNames[locationNamesIndex].getNWS());
//            try
//            {
//                Location loc = locationNames[locationNamesIndex];
//                Logger.getLogger(RetrieveSigStagesImpl.class.getName()).log(Level.FINE, null, "Retrieving " + loc);
//                loc.setSite(fetchSite(fullURL));
//                Logger.getLogger(RetrieveSigStagesImpl.class.getName()).log(Level.FINE, null, "Retrieved " + loc);
//            } catch (Exception ex)
//            {
//                Logger.getLogger(RetrieveSigStagesImpl.class.getName()).log(Level.SEVERE, null, ex);
//            }
//        }
//    }
    
    public void fetchSitesThreaded(SigstageLocation... locationNames)
    {
        Metrics metrics = MetricsServiceProvider.createMetrics("fetchSitesThreaded");
        try(Timer.Context context = metrics.createTimer().start())
        {
            final CountDownLatch cdl = new CountDownLatch(1);
            BlockingQueue<Runnable> threadPoolQueue = new LinkedBlockingQueue<>();
            ThreadPoolExecutor threadPool = new ThreadPoolExecutor(THREADCOUNT / 2, THREADCOUNT, 1, TimeUnit.SECONDS, threadPoolQueue){
                @Override
                protected void terminated()
                {
                    cdl.countDown();
                }
            };
            for(int locationIndex = 0; locationIndex < locationNames.length; locationIndex++)
            {
                final SigstageLocation l = locationNames[locationIndex];
                threadPool.execute(new Runnable(){
                    
                    @Override
                    public void run()
                    {
                        Metrics metrics = MetricsServiceProvider.createMetrics("fetchSitesThreadedRunnable");
                        try(Timer.Context timerContext = metrics.createTimer().start())
                        {
                            String fullURL = DOWNLOADURL.replace("GAGENAME", l.getNWS());
                            try {
                                l.setSite(fetchSite(fullURL));
                            }
                            catch (Exception ex)
                            {
                                System.out.println(l.getNWS());
                                Logger.getLogger(RetrieveSigStagesImpl.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                    }
                });
            }
            threadPool.shutdown();
            try
            {
                cdl.await();
            }
            catch (InterruptedException ex)
            {
                Logger.getLogger(RetrieveSigStagesImpl.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    public Site fetchSite(String fullURL) throws Exception
    {
        Metrics metrics = MetricsServiceProvider.createMetrics("fetchSite");
        try(Timer.Context context = metrics.createTimer().start())
        {
            if(_jaxbContext == null)
            {
                _jaxbContext = JAXBContext.newInstance("usace.rowcps.headless.sigstages.retrieve.xmlmodel");
            }
            Unmarshaller unmarshaller = _jaxbContext.createUnmarshaller();
            URL url = new URL(fullURL);
            InputStream input = url.openStream();
            Site site = ((Site)unmarshaller.unmarshal(input));
            return site;
        }
    }
    
    private static final Pattern VALIDNWS = Pattern.compile("^[A-Za-z]{4}[\\d]$");

    public SigstageLocation[] readLocationFile(Path source) throws FileNotFoundException, IOException
    {
        Metrics metrics = MetricsServiceProvider.createMetrics("readLocationFile");
        try(Timer.Context context = metrics.createTimer().start())
        {
            List<SigstageLocation> locations = new ArrayList<>();
            
            try (BufferedReader reader = new BufferedReader(new FileReader(source.toFile()))) {
                String line;
                while((line = reader.readLine()) != null)
                {
                    line = line.trim();
                    String[] lineArray = line.split(DELIMETER);
                    if(lineArray.length == 0)
                    {
                        continue;
                    }
                    String nws = (lineArray.length > 1) ? lineArray[1] : lineArray[0];
                    if(VALIDNWS.matcher(nws).matches())
                    {
                        String original = lineArray[0];
                        SigstageLocation location = new SigstageLocation(original, nws);
                        locations.add(location);
                    }
                }
            }
            
            SigstageLocation[] locationsArray = new SigstageLocation[locations.size()];
            locationsArray = locations.toArray(locationsArray);
            return locationsArray;
        }
    }
    
    @Override
    public void setParameter(String parameter)
    {
        _parameter = parameter;
    }
    
    @Override
    public String getParameter()
    {
        return _parameter;
    }
    
    @Override
    public void setParameterType(String parameterType)
    {
        _parameterType = parameterType;
    }
    
    @Override
    public String getParameterType()
    {
        return _parameterType;
    }
    
    @Override
    public void setDuration(String duration)
    {
        _duration = duration;
    }
    
    @Override
    public String getDuration()
    {
        return _duration;
    }
    
    @Override
    public void setSpecifiedLevelOverride(Sigstage.Type type, String overrideText)
    {
        _specifiedLevelOverride.put(type, overrideText);
    }
    
    @Override
    public String getSpecifiedLevelOverride(Sigstage.Type type)
    {
        String specifiedLevel = _specifiedLevelOverride.get(type);
        if(specifiedLevel == null)
        {
            specifiedLevel = type.toString();
        }
        return specifiedLevel;
    }
    
    @Override
    public void setOffice(String office)
    {
        _office = office;
    }
    
    @Override
    public String getOffice()
    {
        return _office;
    }
}
