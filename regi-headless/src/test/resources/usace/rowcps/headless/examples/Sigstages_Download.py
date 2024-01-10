sigstates = registry.getCalculation(1.0, "Retrieve Sig States")

#sigstates has the following API exposed:
#
#    public void retrieveSigstages(String sourceFile, String outputFile); // Read names from sourceFile, write CSV to outputFile
#    public void retrieveSigstages(String sourceFile, String outputFile);
#    public void setParameter(String parameter);
#    public String getParameter(String parameter);
#    public void setParameterType(String parameterType);
#    public String getParameter();
#    public void setDuration(String duration);
#    public String getDuration();
#    public void setSpecifiedLevelOverride(Sigstage.Type type, String overrideText);
#    public void getSpecifiedLevelOverride(Sigstage.Type type);
#    public void setOffice(String office);
#    public String getOffice();

inpath = "sites.txt"
outpath = "sigstages.csv"
sigstates.retrieveSigstages(inpath, outpath)

