/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package usace.rowcps.headless.sigstages.importdb;

import usace.rowcps.headless.sigstages.retrieve.*;
import rma.services.annotations.ServiceProvider;
import usace.rowcps.headless.ScriptableCalcFactory;
import usace.rowcps.headless.interfaces.ScriptableCalc;
import usace.rowcps.regi.model.ManagerId;
import usace.rowcps.regi.model.RegiDomain;

/**
 *
 * @author stephen
 */
@ServiceProvider(service = ScriptableCalcFactory.class)
public class ImportSigStagesCalcFactory implements ScriptableCalcFactory
{
    public ImportSigStagesCalcFactory()
    {
    }
    
    @Override
    public String getName()
    {
        return "Import Sig States";
    }

    @Override
    public String getDescription()
    {
        return "Reads a CSV File and then import that data into the database.";
    }

    @Override
    public String getUsage()
    {
        return "";
    }

    @Override
    public ScriptableCalc build(RegiDomain regiDomain, ManagerId managerid)
    {
        return new ScriptableImportSigStagesImpl(regiDomain, managerid);
    }
    
}
