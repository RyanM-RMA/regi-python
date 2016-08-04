/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package usace.rowcps.headless.sigstages.importdb;

import usace.rowcps.headless.sigstages.retrieve.*;
import usace.rowcps.headless.sigstages.retrieve.xmlmodel.Sigstage;

/**
 *
 * @author stephen
 */
public interface ScriptableImportSigstages
{
    public boolean importSigStages(String file);
}
