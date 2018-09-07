/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package usace.rowcps.headless.calculator.status;

import hec.data.basin.IBasin;
import hec.data.location.AssignedLocation;
import hec.data.location.LocationCategoryRef;
import hec.data.location.LocationGroup;
import hec.data.location.LocationGroupRef;
import hec.data.location.LocationTemplate;
import hec.data.project.IProject;
import hec.db.DbConnectionException;
import hec.db.DbException;
import hec.db.DbIoException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.NavigableMap;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import usace.metrics.services.Metrics;
import usace.metrics.services.MetricsServiceProvider;
import usace.metrics.services.Timer;
import usace.rowcps.computation.basinconnectivity.BasinConnectivityModel;
import usace.rowcps.regi.basin.IBasinConnectivityLocation;
import usace.rowcps.regi.basin.IBasinConnectivityModel;
import usace.rowcps.regi.interfaces.model.ManagerIdProvider;
import usace.rowcps.regi.model.AtBasinManager;
import usace.rowcps.regi.model.CacheUsage;
import usace.rowcps.regi.model.OptionalParams;
import usace.rowcps.regi.model.RegiDomain;
import usace.rowcps.regi.status.AtProjectManager;

/**
 *
 * @author josh
 */
public class LocationGroupFactory
{
    ManagerIdProvider _managerIdProvider;
    NavigableMap<LocationTemplate, IProject> _projectsMap;
    Set<LocationGroup> _projectGroups;
    final private BasinConnectivityModel _basinConnectivityModel;
    RegiDomain _regiDomain;
    
    public LocationGroupFactory(ManagerIdProvider managerIdProvider)
    {
        _managerIdProvider = managerIdProvider;
        _basinConnectivityModel = new BasinConnectivityModel(_managerIdProvider.getManagerId());
        _regiDomain = (RegiDomain) RegiDomain.getCurrentProject();
        loadProjectsMap();        
    }        
    
    /**
     * This method returns the populated LocationGroup for "Project Group" 
     * with the given projectGroupId
     * @param projectGroupId
     * @return 
     */
    public LocationGroup retrieveProjectGroup(String projectGroupId)
    {
        LocationGroup retval = null;
        Set<LocationGroup> projectGroups = getProjectGroups();
        for(LocationGroup locGroup : projectGroups)
        {
            if(locGroup.getName().equalsIgnoreCase(projectGroupId))
            {
                retval = locGroup;
                break;
            }
        }
        
        return retval;
    }
    
    private Set<LocationGroup> retrieveProjectGroups()
    {
        AtBasinManager atBasinManager = _regiDomain.getAtBasinManager(_managerIdProvider.getManagerId());
        Set<LocationGroup> projectGroups = new HashSet<>();

        try
        {
            projectGroups.addAll(atBasinManager.getBasinCatalog());
        }
        catch(DbConnectionException | DbIoException ex)
        {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Unable to retrieve ProjectGroup catalog from the database.", ex);
        }

        return projectGroups;
    }
    
    private Set<LocationGroup> getProjectGroups()
    {
        if(_projectGroups != null && _projectGroups.size() > 0)
        {
            return _projectGroups;
        }
        else
        {
            _projectGroups = retrieveProjectGroups();
        }
        
        return _projectGroups;
    }       
    
    public LocationGroup retrieveLocationGroupForBasin(String basinId)
    {
        //try getting the IBasin from the AtBasinManager
        IBasin basin = retrieveIBasin(basinId);
        if(basin == null)
        {
            Logger.getLogger(LocationGroupFactory.class.getName()).log(Level.SEVERE, "Unable to retrieve Basin from CWMS database for BasinId: {0}", basinId);
            return null;
        }
        
        Metrics metrics = MetricsServiceProvider.createMetrics(getClass().getSimpleName(), "getLocationGroup");
        OptionalParams funcParams = new OptionalParams(metrics);
        try(Timer.Context ctx = metrics.createTimer().start())
        {
            LocationGroup lg = new LocationGroup();
            lg.setName(basin.getBasinId());
            lg.setDbOfficeId(basin.getOfficeId());
            lg.setLocationGroupRef(new LocationGroupRef(new LocationCategoryRef(AtBasinManager.BASIN_CATEGORY_REF_ID, basin.getOfficeId()), basin.getOfficeId(), basin.getBasinId()));

            //all stream locations are associated with the 
            LocationTemplate basinLocation = basin.getLocationTemplate();
            
            if(!_basinConnectivityModel.isDataAvailableForLocation(basinLocation))
            {                
                _basinConnectivityModel.fillModel(basin, _projectsMap, funcParams);
            }

            IBasinConnectivityLocation primaryStream = _basinConnectivityModel.getStreamBase(new LocationTemplate(basin.getOfficeId(), basin.getPrimaryStream()));
            
            List<AssignedLocation> assignedLocations = buildAssignedLocations(primaryStream, basinLocation);

            for(int i = 0; i < assignedLocations.size(); i++)
            {
                assignedLocations.get(i).setAttribute(Double.valueOf(i));
            }

            lg.setAssignedLocations(new HashSet<>(assignedLocations));

            return lg;
        }
    }
    
    /**
     * This method returns the basin connectivity model that was populated
     * by the retrieveLocationGroupForBasin() method.     
     * 
     * @return IBasinConnectivityModel
     */
    public IBasinConnectivityModel getBasinConnectivityModel()
    {
        return _basinConnectivityModel;
    }
    
    private List<AssignedLocation> buildAssignedLocations(IBasinConnectivityLocation streamBase, LocationTemplate basinLocation)
    {
            List<AssignedLocation> assignedLocations = new ArrayList<>();        

            LocationTemplate streamBaseLocation = streamBase.getLocationTemplate();

            AssignedLocation aLoc = new AssignedLocation(streamBaseLocation, streamBase.getLocationId(), 0.0, basinLocation);

            aLoc.setDescription("  (" + streamBase.getLocationKind() + ")");

            assignedLocations.add(aLoc);

            streamBase.getStreamBases().forEach(sbase -> assignedLocations.addAll(buildAssignedLocations(sbase, basinLocation)));

            return assignedLocations;
    }
    
    private IBasin retrieveIBasin(String basinId)
    {
        IBasin retval = null;                       
        AtBasinManager atBasinManager = _regiDomain.getAtBasinManager(_managerIdProvider.getManagerId());
        try
        {
            List<IBasin> allBasins = atBasinManager.retrieveAllBasins(CacheUsage.NORMAL);
            for(IBasin iBasin : allBasins)
            {
                if(iBasin.getBasinId().equalsIgnoreCase(basinId))
                {
                    retval = iBasin;
                    break;
                }
            }
        }
        catch (DbException ex)
        {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Error retrieving Basin:", ex);
        }
        return retval;
    }
    
    private NavigableMap<LocationTemplate, IProject> getProjectsMap()
    {
        if(_projectsMap != null)
        {
            return _projectsMap;
        }
        else
        {
            loadProjectsMap();
        }
        return _projectsMap;
    }
    
    private void loadProjectsMap()
    {
        AtBasinManager atBasinManager = _regiDomain.getAtBasinManager(_managerIdProvider.getManagerId());
        AtProjectManager atProjectManager = _regiDomain.getAtProjectManager(_managerIdProvider.getManagerId());
        Metrics metrics = MetricsServiceProvider.createMetrics(getClass().getSimpleName(), "getProejcts");
        OptionalParams funcParams = new OptionalParams(metrics);
        try
        {
            _projectsMap = IBasinConnectivityModel.retrieveAllProjects(atBasinManager, atProjectManager, funcParams);
        }
        catch (DbIoException | DbConnectionException ex)
        {
            Logger.getLogger(LocationGroupFactory.class.getName()).log(Level.SEVERE, "Error loading projects map:", ex);
        }
    }    
}
