from usace.rowcps.headless.tests import TestVariables

exportAssociations = registry.getCalculation(1.0, "Export Associations")

#exportAssociations has the following API exposed:
#
#    public void exportAllTSAssociations(String outputPath, String lineSeparator, String valueSeparator);
#    public void exportTSAssociations(String project, String fileLoc, String lineDelimiter, String valueDelimiter);

print("Exporting all Associations")
outputPath = "Associations.csv"
lineSeparator = "\n"
valueSeparator = "\t"
locs = TestVariables.ALL_PROJECTS
exportAssociations.exportAllTSAssociations(TestVariables.HEADLESS_FILE_LOCATION + outputPath, lineSeparator, valueSeparator)

for loc in locs:
    exportAssociations.exportTSAssociations(loc, TestVariables.HEADLESS_FILE_LOCATION + loc + " " + outputPath, lineSeparator, valueSeparator)
    print("Exported successfully.")
