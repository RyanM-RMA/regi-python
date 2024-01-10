sigstates = registry.getCalculation(1.0, "Import Sig States")

#sigstates has the following API exposed:
#
#    public void importSigStages(String file);

inpath = "sigstages.csv"
sigstates.importSigStages(inpath)
