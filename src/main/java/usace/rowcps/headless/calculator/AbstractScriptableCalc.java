/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package usace.rowcps.headless.calculator;

import usace.rowcps.regi.model.ManagerId;
import usace.rowcps.regi.model.RegiDomain;

/**
 *
 * @author ryan
 */
public abstract class AbstractScriptableCalc
{
	public final RegiDomain regiDomain;
	public final ManagerId managerId;

	public AbstractScriptableCalc(RegiDomain regiDomain, ManagerId manId)
	{
		this.regiDomain = regiDomain;
		this.managerId = manId;
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
}
