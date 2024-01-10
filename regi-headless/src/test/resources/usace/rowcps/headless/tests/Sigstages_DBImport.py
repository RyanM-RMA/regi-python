from usace.rowcps.headless.tests import TestVariables

sigstates = registry.getCalculation(1.0, "Import Sig States")

# sigstates has the following API exposed:
#
#    public void importSigStages(String file);
path = TestVariables.HEADLESS_FILE_LOCATION + "SigStages\\"

inpath = path + "sigstages.csv"
sigstates.importSigStages(inpath)
