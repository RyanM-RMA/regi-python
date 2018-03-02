/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package usace.rowcps.headless.calculator;

import usace.rowcps.regi.interfaces.model.ManagerIdProvider;
import usace.rowcps.regi.model.ManagerId;
import usace.rowcps.regi.model.RegiDomain;
import usace.rowcps.regi.model.SimpleManagerIdProvider;

/**
 *
 * @author ryan
 */
public abstract class AbstractScriptableCalc
{
	public final RegiDomain regiDomain;
	public final ManagerId managerId;
        public final ManagerIdProvider manIdProvider;

	public AbstractScriptableCalc(RegiDomain regiDomain, ManagerId manId)
	{
		this.regiDomain = regiDomain;
		this.managerId = manId;
                this.manIdProvider = new SimpleManagerIdProvider(managerId);
	}

	/**
	 * @return the regiDomain
	 */
	public RegiDomain getRegiDomain()
	{
		return regiDomain;
	}

	/**
	 * @return the managerId
	 */
	public ManagerId getManagerId()
	{
		return managerId;
	}
        
        public ManagerIdProvider getManagerIdProvider()
        {
            return manIdProvider;
        }
}
