/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package usace.rowcps.headless.sigstages.importdb;

import java.util.Date;

/**
 *
 * @author stephen
 */
public interface ScriptableImportSigstages
{
    public boolean importSigStages(String file, Date effectiveDate);
}
