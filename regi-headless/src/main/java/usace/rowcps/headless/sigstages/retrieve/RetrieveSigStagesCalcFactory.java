/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package usace.rowcps.headless.sigstages.retrieve;

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
public class RetrieveSigStagesCalcFactory implements ScriptableCalcFactory
{
    public RetrieveSigStagesCalcFactory()
    {
    }
    
    @Override
    public String getName()
    {
        return "Retrieve Sig States";
    }

    @Override
    public String getDescription()
    {
        return "Reads a newline delimited file containing NWS Names, then writes the sigstages to a CSV";
    }

    @Override
    public String getUsage()
    {
        return "";
    }

    @Override
    public ScriptableCalc build(RegiDomain regiDomain, ManagerId managerid)
    {
        return new RetrieveSigStagesImpl(regiDomain, managerid);
    }
    
}
