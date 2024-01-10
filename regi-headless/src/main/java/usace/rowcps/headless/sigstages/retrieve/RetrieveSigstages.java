/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package usace.rowcps.headless.sigstages.retrieve;

import usace.rowcps.headless.sigstages.retrieve.xmlmodel.Sigstage;

/**
 *
 * @author stephen
 */
public interface RetrieveSigstages
{
    public void retrieveSigstages(String sourceFile, String outputFile, int milliDelay);
    public void setParameter(String parameter);
    public String getParameter();
    public void setParameterType(String parameterType);
    public String getParameterType();
    public void setDuration(String duration);
    public String getDuration();
    public void setSpecifiedLevelOverride(Sigstage.Type type, String overrideText);
    public String getSpecifiedLevelOverride(Sigstage.Type type);
    public void setOffice(String office);
    public String getOffice();
}
