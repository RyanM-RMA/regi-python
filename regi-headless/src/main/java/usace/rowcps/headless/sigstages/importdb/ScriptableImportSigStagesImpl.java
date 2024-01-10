/*
* To change this license header, choose License Headers in Project Properties.
* To change this template file, choose Tools | Templates
* and open the template in the editor.
*/
package usace.rowcps.headless.sigstages.importdb;

import hec.data.level.ILocationLevel;
import hec.db.DbConnectionException;
import hec.db.DbIoException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import usace.rowcps.headless.interfaces.ScriptableCalc;
import usace.rowcps.regi.level.importer.CSVLocationLevelImportUtil;
import usace.rowcps.regi.model.AtLocationLevelManager;
import usace.rowcps.regi.model.RegiDomain;
import usace.rowcps.regi.model.ManagerId;

/**
 *
 * @author stephen
 */
public class ScriptableImportSigStagesImpl implements ScriptableImportSigstages, ScriptableCalc
{
    private RegiDomain _regiDomain;
    private ManagerId _managerId;
    
    public static final String DELIMETER = System.getProperty("sigstages.delim", ";");
    public static final String CSVDELIMETER = System.getProperty("sigstages.csv.delim", "\n");
    public static final String CSVSEPARATOR = System.getProperty("sigstages.csv.separator", ",");
    
    public ScriptableImportSigStagesImpl(RegiDomain regiDomain, ManagerId managerId)
    {
        _regiDomain = regiDomain;
        _managerId = managerId;
    }
    
    @Override
    public boolean importSigStages(String file, Date effectiveDate)
    {
        boolean retval = true;
        AtLocationLevelManager atLocLevelMgr = _regiDomain.getAtLocationLevelManager(_managerId);
        Connection c = null;
        try {
            Path p = Paths.get(file);
            c = atLocLevelMgr.getPooledConnection();
            CSVLocationLevelImportUtil importUtil = new CSVLocationLevelImportUtil();
            importUtil.setPath(p);
            importUtil.readFile(true);
            List<ILocationLevel> locationLevels = importUtil.getLocationLevelList();
            for(int locationLevelIndex = 0; locationLevelIndex < locationLevels.size(); locationLevelIndex++)
            {
                ILocationLevel level = locationLevels.get(locationLevelIndex);
                try {
                    level.setDate(effectiveDate);
                    atLocLevelMgr.addLocationLevel(level);
                } catch (DbConnectionException | DbIoException ex) {
                    Logger.getLogger(ScriptableImportSigStagesImpl.class.getName()).log(Level.SEVERE, null, ex);
                    retval = false;
                    break;
                }
            }
            
            if(retval)
            {
                try {
                    atLocLevelMgr.commitData();
                } catch (DbConnectionException | DbIoException ex) {
                    Logger.getLogger(ScriptableImportSigStagesImpl.class.getName()).log(Level.SEVERE, null, ex);
                    retval = false;
                }
            }
        } catch (DbConnectionException ex) {
            Logger.getLogger(ScriptableImportSigStagesImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
        finally
        {
            try
            {
                if(c != null && !c.isClosed()) c.close();
            }
            catch (SQLException ex)
            {
                Logger.getLogger(ScriptableImportSigStagesImpl.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return retval;
    }
}
