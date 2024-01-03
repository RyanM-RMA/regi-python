from usace.rowcps.headless.tests import TestVariables
sigstages = registry.getCalculation(1.0, "Export Sig States")

# sigstages has the following API exposed:
#
#    public void exportSigStages(String file);
outpath = TestVariables.HEADLESS_FILE_LOCATION + "SigStages\\sites.txt"
sigstages.exportSigStages(outpath)