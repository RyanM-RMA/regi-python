sigstates = registry.getCalculation(1.0, "Retrieve Sig States")

#sigstates has the following API exposed:
#
#    public void retrieveSigstages(String sourceFile, String outputFile, int milliDelay);
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

# Purpose:
# Sigstages_Download.py accesses the AHPS data on the locations listed in
# "sites.txt" and retrieves the relevant sig stage information for each
# location, and stores in the sigstages.csv

inpath = "sites.txt"		# file to read the relevant sig stage sites from
outpath = "sigstages.csv"	# file to write the relevant sig stage information gathered to
accessDelay = 250			# time delay in milliseconds between accesses to the AHPS site
							# the longer the delay, the more likely to avoid timeout issues on connection
							# 250 milliseconds is recommended for best balance between performance and speed
							
sigstates.retrieveSigstages(inpath, outpath, accessDelay)

