sigstages = registry.getCalculation(1.0, "Export Sig States")

#sigstages has the following API exposed:
#
#    public void exportSigStages(String file);

outpath = "sites.txt"
sigstages.exportSigStages(outpath)