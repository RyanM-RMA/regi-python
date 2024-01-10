/*
* To change this license header, choose License Headers in Project Properties.
* To change this template file, choose Tools | Templates
* and open the template in the editor.
*/
package usace.rowcps.headless.sigstages.exportdb;

import hec.data.location.AssignedLocation;
import hec.data.location.LocationGroup;
import hec.data.location.LocationGroupRef;
import hec.data.location.LocationTemplate;
import hec.data.meta.Catalog;
import hec.data.meta.LocationCatalogQuery;
import hec.db.DbConnectionException;
import hec.db.DbIoException;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import usace.rowcps.headless.interfaces.ScriptableCalc;
import usace.rowcps.regi.model.AgencyAlias;
import usace.rowcps.regi.model.AtLocationGroupManager;
import usace.rowcps.regi.model.AtLocationManager;
import usace.rowcps.regi.model.CacheUsage;
import usace.rowcps.regi.model.RegiDomain;
import usace.rowcps.regi.model.ManagerId;

/**
 *
 * @author stephen
 */
public class ScriptableExportSigStagesImpl implements ScriptableExportSigstages, ScriptableCalc
{
    private RegiDomain _regiDomain;
    private ManagerId _managerId;
    private String _office;
    
    public static final String DELIMETER = System.getProperty("sigstages.delim", ";");
    public static final String CSVDELIMETER = System.getProperty("sigstages.csv.delim", "\n");
    public static final String CSVSEPARATOR = System.getProperty("sigstages.csv.separator", ",");
    
    public ScriptableExportSigStagesImpl(RegiDomain regiDomain, ManagerId managerId)
    {
        _regiDomain = regiDomain;
        _managerId = managerId;
        _office = "SWF";
    }
    
    @Override
    public boolean exportSigStages(String file)
    {
        Path toPath = Paths.get(file);
        
        AtLocationManager atLocManager = _regiDomain.getAtLocationManager(_managerId);
        
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(toPath.toFile())))
        {
            Map<LocationTemplate, String> usedIds = new HashMap<>();
            AtLocationGroupManager lgm = _regiDomain.getAtLocationGroupManager(_managerId);
            LocationGroupRef lgr = AgencyAlias.Agency.NWS_HANDBOOK_5_ID.getLocationGroupRef(_regiDomain);
            LocationGroup usgsAliases = lgm.retrieveLocationGroup(lgr, CacheUsage.NORMAL);
            for(AssignedLocation al : usgsAliases.getAssignedLocationsSorted())
            {
                String aliasId = al.getAliasId();
                LocationTemplate location = al.getLocRef();
                usedIds.put(location, aliasId);
            }
            
            Catalog catalog = atLocManager.retrieveLocationCatalog(CacheUsage.NORMAL);
            for(int i=0; i < catalog.size(); i++)
            {
                List rowList = catalog.getRow(i);
                LocationTemplate locationTemplate = LocationCatalogQuery.convertToLocationTemplate(rowList);
                if(locationTemplate != null)
                {
                    String locationId = locationTemplate.getLocationId();
                    String aliasId = usedIds.get(locationTemplate);
                    if(Objects.equals(aliasId, locationId) || aliasId == null || "".equals(aliasId))
                    {
                        writer.write(locationId);
                    }
                    else
                    {
                        writer.write(locationId + DELIMETER + aliasId);
                    }
                    writer.write("\n");
                }
            }
        }
        catch (DbConnectionException|DbIoException|IOException ex)
        {
            Logger.getLogger(ScriptableExportSigStagesImpl.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
        return true;
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
