exportAssociations = registry.getCalculation(1.0, "Export Associations")

#exportAssociations has the following API exposed:
#
#    public void exportAllTSAssociations(String outputPath, String lineSeparator, String valueSeparator);
#    public void exportTSAssociations(String project, String fileLoc, String lineDelimiter, String valueDelimiter);

print("Exporting all Associations")
outputPath = "Associations.csv"
lineSeparator = "\n"
valueSeparator = "\t"
exportAssociations.exportAllTSAssociations(outputPath, lineSeparator, valueSeparator)
print("Exported successfully.")

print("Exporting BDWT2's Associations.")
outputPath = "BDWT2 Associations.csv"
projectName="BDWT2"
exportAssociations.exportTSAssociations(projectName, outputPath, lineSeparator, valueSeparator)
print("Exported successfully.")

print("Exporting SMCT2's Associations.")
outputPath = "SMCT2 Associations.csv"
projectName="SMCT2"
exportAssociations.exportTSAssociations(projectName, outputPath, lineSeparator, valueSeparator)
print("Exported successfully.")

print("Exporting TXKT2's Associations.")
outputPath = "TXKT2 Associations.csv"
projectName="TXKT2"
exportAssociations.exportTSAssociations(projectName, outputPath, lineSeparator, valueSeparator)
print("Exported successfully.")